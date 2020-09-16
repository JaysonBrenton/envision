const matching = require('/data-hub/5/builtins/steps/mastering/default/matching.sjs');
const mastering = require("/com.marklogic.smart-mastering/process-records.xqy");
const masteringStepLib = require("/data-hub/5/builtins/steps/mastering/default/lib.sjs");
// const CollectorLib = require("/data-hub/5/endpoints/collectorLib.sjs");
const DataHub = require("/data-hub/5/datahub.sjs");
const datahub = new DataHub();

var flowName = 'EmployeesMastering'
var flowStep = '3'
var uri = '/envision/datahub/data/CoastalEmployees/55002.json'

let flowDoc= datahub.flow.getFlow(flowName);
const step = flowDoc.steps[flowStep]
const options = step.options

let stepDefinition = datahub.flow.step.getStepByNameAndType(step.stepDefinitionName, step.stepDefinitionType);
let combinedOptions = Object.assign({}, stepDefinition.options, flowDoc.options, step.options);
const query = combinedOptions.sourceQuery
const database = combinedOptions.sourceDatabase || requestParams.database;
options.matchOptions.targetEntity = options.targetEntity
const matchOptions = new NodeBuilder().addNode({ options: options.matchOptions }).toNode();

function findMatches(start = 1, pageLength = 100, limit = 10, summary = {matchSummary: {}}) {
  // console.log(`findMatches[${start}, ${pageLength}]`)
  const uris = xdmp.eval(`fn.subsequence(cts.uris(null, null, ${query}), ${start}, ${pageLength})`, null, {database: xdmp.database(database)})
  if (fn.exists(uris)) {
    const ms = mastering.buildMatchSummary(
      uris,
      matchOptions,
      options.filterQuery ? cts.query(options.filterQuery) : cts.trueQuery(),
      false
    );
    summary = {
      matchSummary: {
        actionDetails: {
          ...(summary.matchSummary.actionDetails || {}),
          ...(ms.matchSummary.actionDetails || {})
        }
      }
    }
    if (fn.count(uris) === pageLength && Object.keys(summary.matchSummary.actionDetails).length < limit) {
      return findMatches(start + pageLength, pageLength, limit, summary)
      //.matchSummary.actionDetails : {})
    }

  }
  return summary
}

findMatches()

// let mergeOptions = new NodeBuilder().addNode({ options: options.mergeOptions }).toNode();
// const matchSummaryJson = matching.main(content, options).value
// const matchSummary = {
//   matchSummary: {
//     ...matchSummaryJson.matchSummary,
//     URIsToProcess: undefined
//   }
// }
// const matchUris = Object.keys(matchSummary.matchSummary.actionDetails)

// const currentUri = matchUris[0]
// let merged = mastering.buildContentObjectsFromMatchSummary(
//   currentUri,
//   matchSummaryJson,
//   mergeOptions,
//   datahub.prov.granularityLevel() === datahub.prov.FINE_LEVEL
// )
// merged
// .find(x => x.uri === currentUri)