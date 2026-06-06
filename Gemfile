source "https://rubygems.org"

gem "fastlane"
# fastlane's google-apis/supply load path requires "multi_json", but recent
# fastlane (2.235.x) and the google-cloud-* gems no longer pull it transitively,
# so `bundle exec fastlane` raises "multi_json is not part of the bundle".
# Declare it explicitly so it resolves into the bundle.
gem "multi_json"
