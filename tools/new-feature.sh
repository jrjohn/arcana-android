#!/usr/bin/env bash
# Generate ONE vertical feature slice in the template's layers (AGENTS.md §2):
#
#   tools/new-feature.sh Invoice
#
#   ui/screens/<Name>Screen.kt            Compose screen (collects output, handles effects)
#   ui/screens/<Name>ViewModel.kt         MVVM Input / Output / Effect, depends on the service interface
#   domain/model/<Name>.kt                pure domain model (no Room / Android imports)
#   domain/service/<Name>Service.kt       business-logic interface (the template's "use case" layer)
#   domain/service/impl/<Name>ServiceImpl.kt
#   domain/repository/<Name>Repository.kt
#   data/repository/impl/<Name>RepositoryImpl.kt   offline-first: Room is the source of truth
#   data/local/entity/<Name>Entity.kt + data/local/dao/<Name>Dao.kt + data/local/mapper/<Name>Mapper.kt
#   data/local/migration/<Name>Migration.kt        Room migration N -> N+1 creating the table
#   di/<Name>Module.kt                    the ONLY file that names the *Impl classes
#   tests: <Name>ServiceImplTest, <Name>RepositoryImplTest, <Name>ViewModelTest
#
# and registers it at the `// new-feature:*` markers in AppDatabase, DatabaseModule and NavGraph
# (route + destination + home-screen entry), plus string resources. Re-running for an existing
# name is refused. The slice is local-only (no Retrofit/Ktorfit API); add a network data source
# under data/network/ and sync it the way OfflineFirstDataRepository does when the feature
# has a backend.
set -euo pipefail

[ $# -eq 1 ] || { echo "usage: $0 <FeatureName>   (PascalCase, e.g. Invoice)" >&2; exit 1; }
NAME="$1"
[[ "$NAME" =~ ^[A-Z][a-z0-9]+([A-Z][a-z0-9]+)*$ ]] || { echo "FeatureName must be PascalCase letters/digits, e.g. Invoice or PurchaseOrder" >&2; exit 1; }

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
NS="$(sed -nE 's/^[[:space:]]*namespace[[:space:]]*=[[:space:]]*"([^"]+)".*/\1/p' app/build.gradle.kts | head -1)"
[ -n "$NS" ] || { echo "cannot read namespace from app/build.gradle.kts" >&2; exit 1; }
PKG="${NS//./\/}"
MAIN="app/src/main/java/$PKG"
TEST="app/src/test/java/$PKG"

SNAKE="$(printf '%s' "$NAME" | sed -E 's/([a-z0-9])([A-Z])/\1_\2/g' | tr '[:upper:]' '[:lower:]')"   # purchase_order
UPPER="$(printf '%s' "$SNAKE" | tr '[:lower:]' '[:upper:]')"                                        # PURCHASE_ORDER
CAMEL="$(printf '%s' "${NAME:0:1}" | tr '[:upper:]' '[:lower:]')${NAME:1}"                         # purchaseOrder
TABLE="${SNAKE}s"
LABEL="$(printf '%s' "$NAME" | sed -E 's/([a-z0-9])([A-Z])/\1 \2/g')"                              # Purchase Order

DB="$MAIN/data/local/AppDatabase.kt"
DBMOD="$MAIN/di/DatabaseModule.kt"
NAV="$MAIN/nav/NavGraph.kt"
for f in "$DB" "$DBMOD" "$NAV"; do
    [ -f "$f" ] || { echo "missing $f" >&2; exit 1; }
done
for m in "new-feature:entities" "new-feature:daos" "new-feature:version"; do
    grep -q "$m" "$DB" || { echo "marker '$m' missing in $DB — restore it (see the template)" >&2; exit 1; }
done
grep -q "new-feature:migrations" "$DBMOD" || { echo "marker 'new-feature:migrations' missing in $DBMOD" >&2; exit 1; }
for m in routes destinations screen-names home-entries; do
    grep -q "new-feature:$m" "$NAV" || { echo "marker 'new-feature:$m' missing in $NAV" >&2; exit 1; }
done
[ ! -e "$MAIN/domain/model/$NAME.kt" ] || { echo "$NAME already exists — refusing to overwrite" >&2; exit 1; }

OLD_VER="$(sed -nE 's/.*version = ([0-9]+), \/\/ new-feature:version.*/\1/p' "$DB")"
[ -n "$OLD_VER" ] || { echo "cannot read Room version from $DB" >&2; exit 1; }
NEW_VER=$((OLD_VER + 1))

render() {  # render <dest>  (template on stdin)
    mkdir -p "$(dirname "$1")"
    sed -e "s/__NS__/$NS/g" -e "s/__Name__/$NAME/g" -e "s/__name__/$CAMEL/g" \
        -e "s/__snake__/$SNAKE/g" -e "s/__UPPER__/$UPPER/g" -e "s/__table__/$TABLE/g" \
        -e "s/__label__/$LABEL/g" -e "s/__from__/$OLD_VER/g" -e "s/__to__/$NEW_VER/g" > "$1"
    echo "  + $1"
}

# insert_before <file> <marker> <text>  — insert <text> (may be multi-line) before the marker line
insert_before() {
    local tmp; tmp="$(mktemp)"
    TEXT="$3" awk -v m="$2" 'index($0, m) && !done { print ENVIRON["TEXT"]; done=1 } { print }' "$1" > "$tmp"
    mv "$tmp" "$1"
}

# add_import <file> <fq-name>  — add after the last import line (no-op if present)
add_import() {
    grep -qx "import $2" "$1" && return 0
    local tmp; tmp="$(mktemp)"
    awk -v imp="import $2" '{ lines[NR]=$0; if ($0 ~ /^import /) last=NR }
        END { for (i=1;i<=NR;i++) { print lines[i]; if (i==last) print imp } }' "$1" > "$tmp"
    mv "$tmp" "$1"
}

echo "generating feature $NAME in $NS (Room $OLD_VER -> $NEW_VER)"

# ---------------------------------------------------------------- domain
render "$MAIN/domain/model/$NAME.kt" <<'EOF'
package __NS__.domain.model

/** Domain model. Pure Kotlin: no Room, Retrofit or Android imports (AGENTS.md R3). */
data class __Name__(
    val id: String,
    val title: String,
    val amountCents: Long,
    val updatedAt: Long = System.currentTimeMillis()
)
EOF

render "$MAIN/domain/repository/${NAME}Repository.kt" <<'EOF'
package __NS__.domain.repository

import __NS__.domain.model.__Name__
import kotlinx.coroutines.flow.Flow

interface __Name__Repository {
    fun observeAll(): Flow<List<__Name__>>
    suspend fun save(item: __Name__): Result<Unit>
    suspend fun delete(id: String): Result<Unit>
}
EOF

render "$MAIN/domain/service/${NAME}Service.kt" <<'EOF'
package __NS__.domain.service

import __NS__.domain.model.__Name__
import kotlinx.coroutines.flow.Flow

interface __Name__Service {
    fun observeAll(): Flow<List<__Name__>>
    suspend fun create(title: String, amountCents: Long): Result<__Name__>
    suspend fun delete(id: String): Result<Unit>
}
EOF

render "$MAIN/domain/service/impl/${NAME}ServiceImpl.kt" <<'EOF'
package __NS__.domain.service.impl

import __NS__.domain.model.__Name__
import __NS__.domain.repository.__Name__Repository
import __NS__.domain.service.__Name__Service
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

/** Business rules for __Name__ live here — not in the ViewModel, not in the repository. */
class __Name__ServiceImpl @Inject constructor(
    private val repository: __Name__Repository
) : __Name__Service {

    override fun observeAll(): Flow<List<__Name__>> = repository.observeAll()

    override suspend fun create(title: String, amountCents: Long): Result<__Name__> {
        if (title.isBlank()) return Result.failure(IllegalArgumentException("title must not be blank"))
        if (amountCents < 0) return Result.failure(IllegalArgumentException("amount must not be negative"))
        val item = __Name__(id = UUID.randomUUID().toString(), title = title.trim(), amountCents = amountCents)
        return repository.save(item).map { item }
    }

    override suspend fun delete(id: String): Result<Unit> = repository.delete(id)
}
EOF

# ---------------------------------------------------------------- data
render "$MAIN/data/local/entity/${NAME}Entity.kt" <<'EOF'
package __NS__.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "__table__")
data class __Name__Entity(
    @PrimaryKey val id: String,
    val title: String,
    val amountCents: Long,
    val updatedAt: Long
)
EOF

render "$MAIN/data/local/mapper/${NAME}Mapper.kt" <<'EOF'
package __NS__.data.local.mapper

import __NS__.data.local.entity.__Name__Entity
import __NS__.domain.model.__Name__

fun __Name__Entity.toDomain(): __Name__ =
    __Name__(id = id, title = title, amountCents = amountCents, updatedAt = updatedAt)

fun __Name__.toEntity(): __Name__Entity =
    __Name__Entity(id = id, title = title, amountCents = amountCents, updatedAt = updatedAt)
EOF

render "$MAIN/data/local/dao/${NAME}Dao.kt" <<'EOF'
package __NS__.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import __NS__.data.local.entity.__Name__Entity
import kotlinx.coroutines.flow.Flow

@Dao
interface __Name__Dao {
    @Query("SELECT * FROM __table__ ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<__Name__Entity>>

    @Upsert
    suspend fun upsert(entity: __Name__Entity)

    @Query("DELETE FROM __table__ WHERE id = :id")
    suspend fun deleteById(id: String)
}
EOF

render "$MAIN/data/local/migration/${NAME}Migration.kt" <<'EOF'
package __NS__.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Creates the __table__ table (Room schema __from__ -> __to__). Must match __Name__Entity exactly. */
val MIGRATION___from_____to__ = object : Migration(__from__, __to__) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `__table__` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                "`amountCents` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
    }
}
EOF

render "$MAIN/data/repository/impl/${NAME}RepositoryImpl.kt" <<'EOF'
package __NS__.data.repository.impl

import __NS__.data.local.dao.__Name__Dao
import __NS__.data.local.mapper.toDomain
import __NS__.data.local.mapper.toEntity
import __NS__.domain.model.__Name__
import __NS__.domain.repository.__Name__Repository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Offline-first: Room is the single source of truth; the UI observes the DAO flow. */
class __Name__RepositoryImpl @Inject constructor(
    private val dao: __Name__Dao
) : __Name__Repository {

    override fun observeAll(): Flow<List<__Name__>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun save(item: __Name__): Result<Unit> = runCatching { dao.upsert(item.toEntity()) }

    override suspend fun delete(id: String): Result<Unit> = runCatching { dao.deleteById(id) }
}
EOF

# ---------------------------------------------------------------- di
render "$MAIN/di/${NAME}Module.kt" <<'EOF'
package __NS__.di

import __NS__.data.local.AppDatabase
import __NS__.data.local.dao.__Name__Dao
import __NS__.data.repository.impl.__Name__RepositoryImpl
import __NS__.domain.repository.__Name__Repository
import __NS__.domain.service.__Name__Service
import __NS__.domain.service.impl.__Name__ServiceImpl
import dagger.Binds
import dagger.Module // NOSONAR kotlin:S1128
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** The only place that names __Name__'s implementation classes (AGENTS.md R2). */
@Module
@InstallIn(SingletonComponent::class)
abstract class __Name__Module {

    @Binds
    @Singleton
    abstract fun bind__Name__Repository(impl: __Name__RepositoryImpl): __Name__Repository

    @Binds
    @Singleton
    abstract fun bind__Name__Service(impl: __Name__ServiceImpl): __Name__Service

    companion object {
        @Provides
        fun provide__Name__Dao(appDatabase: AppDatabase): __Name__Dao = appDatabase.__name__Dao()
    }
}
EOF

# ---------------------------------------------------------------- ui
render "$MAIN/ui/screens/${NAME}ViewModel.kt" <<'EOF'
package __NS__.ui.screens

import androidx.lifecycle.viewModelScope
import __NS__.R
import __NS__.core.analytics.AnalyticsTracker
import __NS__.core.analytics.AnalyticsViewModel
import __NS__.core.common.StringProvider
import __NS__.domain.model.__Name__
import __NS__.domain.service.__Name__Service
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class __Name__ViewModel @Inject constructor(
    private val __name__Service: __Name__Service,
    private val stringProvider: StringProvider,
    analyticsTracker: AnalyticsTracker
) : AnalyticsViewModel(analyticsTracker) {

    // ============================================
    // Input - Events from UI to ViewModel
    // ============================================
    sealed interface Input {
        data class Create(val title: String, val amountCents: Long) : Input
        data class Delete(val id: String) : Input
    }

    // ============================================
    // Output - UI State for binding
    // ============================================
    data class Output(
        val items: List<__Name__> = emptyList(),
        val isLoading: Boolean = true
    )

    // ============================================
    // Effect - One-time events from ViewModel to UI
    // ============================================
    sealed interface Effect {
        data class ShowError(val message: String) : Effect
        data class ShowSuccess(val message: String) : Effect
    }

    private val _output = MutableStateFlow(Output())
    val output: StateFlow<Output> = _output.asStateFlow()

    private val _effect = Channel<Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            __name__Service.observeAll().collect { items ->
                _output.update { it.copy(items = items, isLoading = false) }
            }
        }
    }

    // ============================================
    // Event Handler
    // ============================================
    fun onEvent(input: Input) {
        when (input) {
            is Input.Create -> create(input.title, input.amountCents)
            is Input.Delete -> delete(input.id)
        }
    }

    private fun create(title: String, amountCents: Long) {
        viewModelScope.launch {
            val result = __name__Service.create(title, amountCents)
            _effect.send(
                if (result.isSuccess) Effect.ShowSuccess(stringProvider.getString(R.string.__snake___saved_success))
                else Effect.ShowError(stringProvider.getString(R.string.__snake___save_failed))
            )
        }
    }

    private fun delete(id: String) {
        viewModelScope.launch {
            val result = __name__Service.delete(id)
            _effect.send(
                if (result.isSuccess) Effect.ShowSuccess(stringProvider.getString(R.string.__snake___deleted_success))
                else Effect.ShowError(stringProvider.getString(R.string.__snake___delete_failed))
            )
        }
    }
}
EOF

render "$MAIN/ui/screens/${NAME}Screen.kt" <<'EOF'
package __NS__.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import __NS__.R

/** View: renders [__Name__ViewModel.Output], sends [__Name__ViewModel.Input], shows effects. No logic. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun __Name__Screen(
    viewModel: __Name__ViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.output.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is __Name__ViewModel.Effect.ShowError -> snackbarHostState.showSnackbar(effect.message)
                is __Name__ViewModel.Effect.ShowSuccess -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.__snake___title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.__snake___field_title)) },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.__snake___field_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            Button(
                onClick = {
                    viewModel.onEvent(__Name__ViewModel.Input.Create(title, amount.toLongOrNull() ?: 0L))
                    title = ""
                    amount = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.__snake___add)) }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.items, key = { it.id }) { item ->
                    ListItem(
                        headlineContent = { Text(item.title) },
                        supportingContent = { Text(item.amountCents.toString()) },
                        trailingContent = {
                            IconButton(onClick = { viewModel.onEvent(__Name__ViewModel.Input.Delete(item.id)) }) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            }
                        }
                    )
                }
            }
        }
    }
}
EOF

# ---------------------------------------------------------------- tests
render "$TEST/domain/service/impl/${NAME}ServiceImplTest.kt" <<'EOF'
package __NS__.domain.service.impl

import __NS__.domain.model.__Name__
import __NS__.domain.repository.__Name__Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class __Name__ServiceImplTest {

    private lateinit var repository: __Name__Repository
    private lateinit var service: __Name__ServiceImpl

    @Before
    fun setup() {
        repository = mock()
        service = __Name__ServiceImpl(repository)
    }

    @Test
    fun `observeAll returns the repository flow`() = runTest {
        val flow = flowOf(listOf(__Name__(id = "1", title = "a", amountCents = 1)))
        whenever(repository.observeAll()).thenReturn(flow)
        assertEquals(flow, service.observeAll())
    }

    @Test
    fun `create saves a trimmed item`() = runTest {
        whenever(repository.save(any())).thenReturn(Result.success(Unit))
        val result = service.create("  first  ", 1200)
        assertTrue(result.isSuccess)
        assertEquals("first", result.getOrThrow().title)
        assertEquals(1200, result.getOrThrow().amountCents)
    }

    @Test
    fun `create rejects a blank title without touching the repository`() = runTest {
        assertTrue(service.create(" ", 1).isFailure)
        verify(repository, never()).save(any())
    }

    @Test
    fun `create rejects a negative amount`() = runTest {
        assertTrue(service.create("x", -1).isFailure)
        verify(repository, never()).save(any())
    }

    @Test
    fun `create propagates a repository failure`() = runTest {
        whenever(repository.save(any())).thenReturn(Result.failure(IllegalStateException("disk")))
        assertTrue(service.create("x", 1).isFailure)
    }

    @Test
    fun `delete delegates to the repository`() = runTest {
        whenever(repository.delete("1")).thenReturn(Result.success(Unit))
        assertTrue(service.delete("1").isSuccess)
        verify(repository).delete("1")
    }
}
EOF

render "$TEST/data/repository/${NAME}RepositoryImplTest.kt" <<'EOF'
package __NS__.data.repository

import __NS__.data.local.dao.__Name__Dao
import __NS__.data.local.entity.__Name__Entity
import __NS__.data.repository.impl.__Name__RepositoryImpl
import __NS__.domain.model.__Name__
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class __Name__RepositoryImplTest {

    private lateinit var dao: __Name__Dao
    private lateinit var repository: __Name__RepositoryImpl

    @Before
    fun setup() {
        dao = mock()
        repository = __Name__RepositoryImpl(dao)
    }

    @Test
    fun `observeAll maps entities to domain`() = runTest {
        whenever(dao.observeAll()).thenReturn(flowOf(listOf(__Name__Entity("1", "a", 5, 9))))
        assertEquals(listOf(__Name__("1", "a", 5, 9)), repository.observeAll().first())
    }

    @Test
    fun `save upserts the entity`() = runTest {
        assertTrue(repository.save(__Name__("1", "a", 5, 9)).isSuccess)
        verify(dao).upsert(__Name__Entity("1", "a", 5, 9))
    }

    @Test
    fun `save turns a dao exception into a failure`() = runTest {
        whenever(dao.upsert(any())).thenThrow(IllegalStateException("disk"))
        assertTrue(repository.save(__Name__("1", "a", 5, 9)).isFailure)
    }

    @Test
    fun `delete removes by id`() = runTest {
        assertTrue(repository.delete("1").isSuccess)
        verify(dao).deleteById("1")
    }
}
EOF

render "$TEST/ui/screens/${NAME}ViewModelTest.kt" <<'EOF'
package __NS__.ui.screens

import app.cash.turbine.test
import __NS__.R
import __NS__.core.analytics.AnalyticsTracker
import __NS__.core.common.StringProvider
import __NS__.domain.model.__Name__
import __NS__.domain.service.__Name__Service
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class __Name__ViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var service: __Name__Service
    private lateinit var stringProvider: StringProvider
    private lateinit var analyticsTracker: AnalyticsTracker
    private val items = listOf(__Name__(id = "1", title = "a", amountCents = 5))

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        service = mock()
        stringProvider = mock()
        analyticsTracker = mock()
        whenever(service.observeAll()).thenReturn(flowOf(items))
        whenever(stringProvider.getString(R.string.__snake___saved_success)).thenReturn("saved")
        whenever(stringProvider.getString(R.string.__snake___save_failed)).thenReturn("save failed")
        whenever(stringProvider.getString(R.string.__snake___deleted_success)).thenReturn("deleted")
        whenever(stringProvider.getString(R.string.__snake___delete_failed)).thenReturn("delete failed")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = __Name__ViewModel(service, stringProvider, analyticsTracker)

    @Test
    fun `output exposes the observed items`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(items, vm.output.value.items)
        assertFalse(vm.output.value.isLoading)
    }

    @Test
    fun `create success emits ShowSuccess`() = runTest {
        whenever(service.create("b", 7)).thenReturn(Result.success(__Name__("2", "b", 7)))
        val vm = viewModel()
        vm.effect.test {
            vm.onEvent(__Name__ViewModel.Input.Create("b", 7))
            assertEquals(__Name__ViewModel.Effect.ShowSuccess("saved"), awaitItem())
        }
        verify(service).create("b", 7)
    }

    @Test
    fun `create failure emits ShowError`() = runTest {
        whenever(service.create("", 7)).thenReturn(Result.failure(IllegalArgumentException()))
        val vm = viewModel()
        vm.effect.test {
            vm.onEvent(__Name__ViewModel.Input.Create("", 7))
            assertEquals(__Name__ViewModel.Effect.ShowError("save failed"), awaitItem())
        }
    }

    @Test
    fun `delete success emits ShowSuccess`() = runTest {
        whenever(service.delete("1")).thenReturn(Result.success(Unit))
        val vm = viewModel()
        vm.effect.test {
            vm.onEvent(__Name__ViewModel.Input.Delete("1"))
            assertEquals(__Name__ViewModel.Effect.ShowSuccess("deleted"), awaitItem())
        }
    }

    @Test
    fun `delete failure emits ShowError`() = runTest {
        whenever(service.delete("1")).thenReturn(Result.failure(IllegalStateException()))
        val vm = viewModel()
        vm.effect.test {
            vm.onEvent(__Name__ViewModel.Input.Delete("1"))
            assertEquals(__Name__ViewModel.Effect.ShowError("delete failed"), awaitItem())
        }
    }
}
EOF

# ---------------------------------------------------------------- registration
add_import "$DB" "$NS.data.local.dao.${NAME}Dao"
add_import "$DB" "$NS.data.local.entity.${NAME}Entity"
insert_before "$DB" "new-feature:entities" "        ${NAME}Entity::class,"
insert_before "$DB" "new-feature:daos" "    abstract fun ${CAMEL}Dao(): ${NAME}Dao"
sed -i.bak -E "s/version = ${OLD_VER}, \/\/ new-feature:version/version = ${NEW_VER}, \/\/ new-feature:version/" "$DB" && rm -f "$DB.bak"
echo "  ~ $DB (entity, dao, version $OLD_VER -> $NEW_VER)"

add_import "$DBMOD" "$NS.data.local.migration.MIGRATION_${OLD_VER}_${NEW_VER}"
sed -i.bak -E "s/\) \/\/ new-feature:migrations/, MIGRATION_${OLD_VER}_${NEW_VER}) \/\/ new-feature:migrations/" "$DBMOD" && rm -f "$DBMOD.bak"
echo "  ~ $DBMOD (MIGRATION_${OLD_VER}_${NEW_VER})"

add_import "$NAV" "$NS.ui.screens.HomeEntry"
add_import "$NAV" "$NS.ui.screens.${NAME}Screen"
insert_before "$NAV" "new-feature:routes" "    const val ${UPPER} = \"${SNAKE}\""
insert_before "$NAV" "new-feature:screen-names" "                route == Routes.${UPPER} -> \"${SNAKE}\""
insert_before "$NAV" "new-feature:home-entries" "                    HomeEntry(\"${LABEL}\") { navController.navigate(Routes.${UPPER}) },"
insert_before "$NAV" "new-feature:destinations" "        composable(Routes.${UPPER}) {
            ${NAME}Screen(onNavigateBack = { navController.popBackStack() })
        }"
echo "  ~ $NAV (route, destination, home entry)"

for res in app/src/main/res/values/strings.xml app/src/main/res/values-*/strings.xml; do
    [ -f "$res" ] || continue
    grep -q "name=\"${SNAKE}_title\"" "$res" && continue
    insert_before "$res" "</resources>" "    <!-- ${NAME} (tools/new-feature.sh) -->
    <string name=\"${SNAKE}_title\">${LABEL}</string>
    <string name=\"${SNAKE}_field_title\">Title</string>
    <string name=\"${SNAKE}_field_amount\">Amount (cents)</string>
    <string name=\"${SNAKE}_add\">Add</string>
    <string name=\"${SNAKE}_saved_success\">${LABEL} saved</string>
    <string name=\"${SNAKE}_save_failed\">Could not save ${LABEL}</string>
    <string name=\"${SNAKE}_deleted_success\">${LABEL} deleted</string>
    <string name=\"${SNAKE}_delete_failed\">Could not delete ${LABEL}</string>"
    echo "  ~ $res"
done

cat <<EOF

done. Next (AGENTS.md §5):
  ./gradlew testDebugUnitTest assembleDebug
  tools/arch-check.sh
Then replace the placeholder fields (title, amountCents) with the real ones in ALL of:
${NAME}.kt, ${NAME}Entity.kt, ${NAME}Mapper.kt, ${NAME}Migration.kt (SQL must match the entity), tests.
EOF
