package com.marklogic.envision.dataServices;

// IMPORTANT: Do not edit. This file is generated.

import com.marklogic.client.io.Format;
import com.marklogic.client.io.marker.AbstractWriteHandle;


import com.marklogic.client.DatabaseClient;

import com.marklogic.client.impl.BaseProxy;

/**
 * Provides a set of operations on the database server
 */
public interface EntitySearcher {
    /**
     * Creates a EntitySearcher object for executing operations on the database server.
     *
     * The DatabaseClientFactory class can create the DatabaseClient parameter. A single
     * client object can be used for any number of requests and in multiple threads.
     *
     * @param db	provides a client for communicating with the database server
     * @return	an object for session state
     */
    static EntitySearcher on(DatabaseClient db) {
        final class EntitySearcherImpl implements EntitySearcher {
            private BaseProxy baseProxy;

            private EntitySearcherImpl(DatabaseClient dbClient) {
                baseProxy = new BaseProxy(dbClient, "/envision/search/");
            }

            @Override
            public com.fasterxml.jackson.databind.JsonNode findEntities(com.fasterxml.jackson.databind.JsonNode model, String qtext, Integer page, Integer pageLength, String sort, String searchQuery) {
              return BaseProxy.JsonDocumentType.toJsonNode(
                baseProxy
                .request("findEntities.sjs", BaseProxy.ParameterValuesKind.MULTIPLE_MIXED)
                .withSession()
                .withParams(
                    BaseProxy.documentParam("model", false, BaseProxy.JsonDocumentType.fromJsonNode(model)),
                    BaseProxy.atomicParam("qtext", true, BaseProxy.StringType.fromString(qtext)),
                    BaseProxy.atomicParam("page", false, BaseProxy.IntegerType.fromInteger(page)),
                    BaseProxy.atomicParam("pageLength", false, BaseProxy.IntegerType.fromInteger(pageLength)),
                    BaseProxy.atomicParam("sort", true, BaseProxy.StringType.fromString(sort)),
                    BaseProxy.atomicParam("searchQuery", false, BaseProxy.StringType.fromString(searchQuery)))
                .withMethod("POST")
                .responseSingle(false, Format.JSON)
                );
            }


            @Override
            public com.fasterxml.jackson.databind.JsonNode relatedEntitiesToConcept(String concept, Integer page, Integer pageLength) {
              return BaseProxy.JsonDocumentType.toJsonNode(
                baseProxy
                .request("relatedEntitiesToConcept.sjs", BaseProxy.ParameterValuesKind.MULTIPLE_ATOMICS)
                .withSession()
                .withParams(
                    BaseProxy.atomicParam("concept", false, BaseProxy.StringType.fromString(concept)),
                    BaseProxy.atomicParam("page", false, BaseProxy.IntegerType.fromInteger(page)),
                    BaseProxy.atomicParam("pageLength", false, BaseProxy.IntegerType.fromInteger(pageLength)))
                .withMethod("POST")
                .responseSingle(false, Format.JSON)
                );
            }


            @Override
            public com.fasterxml.jackson.databind.JsonNode relatedEntities(com.fasterxml.jackson.databind.JsonNode model, String uri, String label, Integer page, Integer pageLength) {
              return BaseProxy.JsonDocumentType.toJsonNode(
                baseProxy
                .request("relatedEntities.sjs", BaseProxy.ParameterValuesKind.MULTIPLE_MIXED)
                .withSession()
                .withParams(
                    BaseProxy.documentParam("model", false, BaseProxy.JsonDocumentType.fromJsonNode(model)),
                    BaseProxy.atomicParam("uri", false, BaseProxy.StringType.fromString(uri)),
                    BaseProxy.atomicParam("label", false, BaseProxy.StringType.fromString(label)),
                    BaseProxy.atomicParam("page", false, BaseProxy.IntegerType.fromInteger(page)),
                    BaseProxy.atomicParam("pageLength", false, BaseProxy.IntegerType.fromInteger(pageLength)))
                .withMethod("POST")
                .responseSingle(false, Format.JSON)
                );
            }


            @Override
            public com.fasterxml.jackson.databind.JsonNode getValues(String facetName, String qtext, String searchQuery) {
              return BaseProxy.JsonDocumentType.toJsonNode(
                baseProxy
                .request("getValues.sjs", BaseProxy.ParameterValuesKind.MULTIPLE_ATOMICS)
                .withSession()
                .withParams(
                    BaseProxy.atomicParam("facetName", false, BaseProxy.StringType.fromString(facetName)),
                    BaseProxy.atomicParam("qtext", true, BaseProxy.StringType.fromString(qtext)),
                    BaseProxy.atomicParam("searchQuery", false, BaseProxy.StringType.fromString(searchQuery)))
                .withMethod("POST")
                .responseSingle(false, Format.JSON)
                );
            }

        }

        return new EntitySearcherImpl(db);
    }

  /**
   * Invokes the findEntities operation on the database server
   *
   * @param model	provides input
   * @param qtext	provides input
   * @param page	provides input
   * @param pageLength	provides input
   * @param sort	provides input
   * @param searchQuery	provides input
   * @return	as output
   */
    com.fasterxml.jackson.databind.JsonNode findEntities(com.fasterxml.jackson.databind.JsonNode model, String qtext, Integer page, Integer pageLength, String sort, String searchQuery);

  /**
   * Invokes the relatedEntitiesToConcept operation on the database server
   *
   * @param concept	provides input
   * @param page	provides input
   * @param pageLength	provides input
   * @return	as output
   */
    com.fasterxml.jackson.databind.JsonNode relatedEntitiesToConcept(String concept, Integer page, Integer pageLength);

  /**
   * Invokes the relatedEntities operation on the database server
   *
   * @param model	provides input
   * @param uri	provides input
   * @param label	provides input
   * @param page	provides input
   * @param pageLength	provides input
   * @return	as output
   */
    com.fasterxml.jackson.databind.JsonNode relatedEntities(com.fasterxml.jackson.databind.JsonNode model, String uri, String label, Integer page, Integer pageLength);

  /**
   * Invokes the getValues operation on the database server
   *
   * @param facetName	provides input
   * @param qtext	provides input
   * @param searchQuery	provides input
   * @return	as output
   */
    com.fasterxml.jackson.databind.JsonNode getValues(String facetName, String qtext, String searchQuery);

}
