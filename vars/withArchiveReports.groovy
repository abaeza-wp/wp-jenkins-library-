def call() {
    steps
    {
        script
        {
            load("deployment/boilerplate/scripts/pipeline/reporting.groovy").archiveReports()
        }
    }
}

