package liquibase.extension.testing.command

CommandTests.define {
    command = ["futureRollbackFromTagSQL"]
    signature = """
Short Description: Generates SQL to revert future changes up to the specified tag
Long Description: Generates SQL to revert future changes up to the specified tag
Required Args:
  tag (String) Tag ID to rollback from
  url (String) The JDBC database connection URL
Optional Args:
  changeLogFile (String) The root changelog
    Default: null
  contexts (String) Changeset contexts to match
    Default: null
  labels (String) Changeset labels to match
    Default: null
  password (String) Password to use to connect to the database
    Default: null
  username (String) Username to use to connect to the database
    Default: null
"""

    run {
        arguments = [
                tag          : "version_2.0",
                changeLogFile: "changelogs/hsqldb/complete/rollback.tag.changelog.xml",
        ]


        setup {
            runChangelog "changelogs/hsqldb/complete/rollback.tag.changelog.xml"
            rollback 5, "changelogs/hsqldb/complete/rollback.tag.changelog.xml"
        }

        expectedResults = [
                statusMessage: "Successfully executed futureRollbackFromTagSQL",
                statusCode   : 0
        ]
    }
}
