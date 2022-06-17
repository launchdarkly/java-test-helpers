# Change log

All notable changes to the project will be documented in this file. This project adheres to [Semantic Versioning](http://semver.org).

## [1.1.1] - 2022-06-17
### Fixed:
- Fixed Hamcrest dependency to use `hamcrest-library` rather than `hamcrest-all`, because JUnit (which is commonly used in any unit test code that would also use Hamcrest) has a transitive dependency on `hamcrest-library` and using both would result in duplication on the classpath.

## [1.1.0] - 2021-07-21
### Added:
- `Assertions`, `ConcurrentHelpers`, `JsonAssertions`, `TempDir`, `TempFile`, `TypeBehavior`.

## [1.0.0] - 2021-06-25
Initial release.
