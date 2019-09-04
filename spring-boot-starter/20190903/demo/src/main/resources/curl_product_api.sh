#!/usr/bin/env bash
#
# The use of the software described here is subject to the DataStax Labs Terms
# [https://www.datastax.com/terms/datastax-labs-terms]
#

# create an entity
curl -d '{"id":1, "description":"i was created by curl"}' -H "Content-Type: application/json" -X POST http://localhost:8080/product/

# retrieve
curl -X GET http://localhost:8080/product/1

# delete
curl -X DELETE http://localhost:8080/product/1

# retrieve - NOT_FOUND
curl -X GET http://localhost:8080/product/1