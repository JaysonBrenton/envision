'use strict'

const ents = require('/envision/entities.sjs');
const modelWrapper = require('/envision/model.sjs');

var model;
var qtext;
var page;
var pageLength;
var sort;
var searchQuery;

model = modelWrapper(model);
let start = ((page - 1) * pageLength) + 1;

let query = fn.head(xdmp.unquote(searchQuery)).root
const sorted = ents.runQuery(qtext, query, {start, pageLength, sort})
const uris = sorted.results.map(r => r.uri)
const connectionLimit = 100
const graphItems = ents.getEntities(model, uris, { connectionLimit, sort })
const results = sorted.results.map(result => {
	const item = graphItems.nodes[result.uri] || {};
	return {
		...result,
		...item
	}
})

const result = {
	page: page,
	total: sorted.total,
	pageLength: pageLength,
	...graphItems,
	results: results,
	facets: sorted.facets
}

result;
