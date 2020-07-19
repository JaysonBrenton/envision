'use strict';

var flowsToGet;

flowsToGet = flowsToGet.toObject();

const config = require("/com.marklogic.hub/config.sjs")
const Jobs = require('/data-hub/5/impl/jobs.sjs')

let flows = fn.collection(['http://marklogic.com/data-hub/flow']).toArray()
	.filter(flow => !xdmp.nodeUri(flow).match('/default-'))
	.map(f => f.toObject())

if (flowsToGet) {
	flows = flows.filter(flow => flowsToGet.indexOf(flow.name) >= 0)
}

const flowNames = flows.map(flow => flow.name)

const jobs = fn.head(xdmp.invokeFunction(function() {
	const jobs = new Jobs()
	return jobs.getJobDocsForFlows(flowNames)
}, {
	database: xdmp.database(config.JOBDATABASE)
}))

flows.map(flow => {
	const jobInfo = jobs[flow.name]
	return {
		...flow,
		jobs: jobInfo.jobIds,
		latestJob: jobInfo.latestJob ? jobInfo.latestJob.job : {}
	}
})
