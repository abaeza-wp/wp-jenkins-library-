def call() {
    script
    {
        load("deployment/boilerplate/scripts/pipeline/reporting.groovy").archiveReports()
    }
}

