Elasticsearch Beyonder
======================

Welcome to the [Elasticsearch](http://www.elastic.co/) Beyonder project.

This project comes historically from [spring-elasticsearch](https://github.com/dadoonet/spring-elasticsearch) project.


Versions
========

| elasticsearch-beyonder  | elasticsearch | Release date |
|:-----------------------:|:-------------:|:------------:|
| 7.6-SNAPSHOT            | 7.x           |              |
| 7.5                     | 7.x           |  2020-01-15  |
| 7.0                     | 7.0 -> 7.x    |  2019-04-04  |
| 6.5                     | 6.5 -> 6.x    |  2019-01-04  |
| 6.3                     | 6.3 -> 6.4    |  2018-07-21  |
| 6.0                     | 6.0 -> 6.2    |  2018-02-05  |
| 5.1                     | 5.x, 6.x      |  2017-07-12  |
| 5.0                     | 5.x, 6.x      |  2017-07-11  |
| 2.1.0                   | 2.0, 2.1      |  2015-11-25  |
| 2.0.0                   |      2.0      |  2015-10-24  |
| 1.5.0                   |      1.5      |  2015-03-27  |
| 1.4.1                   |      1.4      |  2015-03-02  |
| 1.4.0                   |      1.4      |  2015-02-27  |


Documentation
=============

* For 7.x elasticsearch versions, you are reading the latest documentation.
* For 6.x elasticsearch versions, look at [es-6.x branch](https://github.com/dadoonet/elasticsearch-beyonder/tree/es-6.x).
* For 5.x elasticsearch versions, look at [es-5.x branch](https://github.com/dadoonet/elasticsearch-beyonder/tree/es-5.x).
* For 2.x elasticsearch versions, look at [es-2.1 branch](https://github.com/dadoonet/elasticsearch-beyonder/tree/es-2.1).

Build Status
============

Thanks to Travis for the [build status](https://travis-ci.org/dadoonet/elasticsearch-beyonder): 
[![Build Status](https://travis-ci.org/dadoonet/elasticsearch-beyonder.svg)](https://travis-ci.org/dadoonet/elasticsearch-beyonder)


Getting Started
===============

Maven dependency
----------------

Import elasticsearch-beyonder in you project `pom.xml` file:

```xml
<dependency>
  <groupId>fr.pilato.elasticsearch</groupId>
  <artifactId>elasticsearch-beyonder</artifactId>
  <version>7.5</version>
</dependency>
```

You need to import as well the elasticsearch client you want to use by adding one of the following
dependencies to your `pom.xml` file.

For example, here is how to import the REST Client to your project:

```xml
<dependency>
    <groupId>org.elasticsearch.client</groupId>
    <artifactId>elasticsearch-rest-client</artifactId>
    <version>7.5.1</version>
</dependency>
```

For example, here is how to import the Transport Client to your project (deprecated):

```xml
<dependency>
    <groupId>org.elasticsearch.client</groupId>
    <artifactId>transport</artifactId>
    <version>7.5.1</version>
</dependency>
```


Adding Beyonder to your client
------------------------------

For RestClient or TransportClient, you can define many properties to manage automatic creation
of index, mappings, templates and aliases.

To activate those features, you only need to call:

```java
ElasticsearchBeyonder.start(client);
```

By default, Beyonder will try to locate resources from `elasticsearch` directory within your classpath.
We will use this default value for the rest of the documentation.

But you can change this using:

```java
ElasticsearchBeyonder.start(client, "models/myelasticsearch");
```

In that case, Beyonder will search for resources from `models/myelasticsearch`.


## Using REST Client (recommended)

Elasticsearch provides a [Rest Client](https://www.elastic.co/guide/en/elasticsearch/client/java-rest/7.0/index.html).
It's the recommended way as the Transport Client is now deprecated and will be removed in a next major version.

Just pass to Beyonder a Rest Client instance:

```java
RestClient client = RestClient.builder(HttpHost.create("http://127.0.0.1:9200")).build();
ElasticsearchBeyonder.start(client);
```

For the record, you can also use X-Pack security with:

```java
CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("elastic", "changeme"));
RestClient client = RestClient.builder(HttpHost.create("http://127.0.0.1:9200"))
        .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
            @Override
            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }
        }).build();
ElasticsearchBeyonder.start(client);
```

## Using Transport Client (deprecated)

To use the deprecated TransportClient, just pass it to Beyonder:

```java
Client client = new PreBuiltTransportClient(Settings.EMPTY)
           .addTransportAddress(new InetSocketTransportAddress(new InetSocketAddress("127.0.0.1", 9300)));
ElasticsearchBeyonder.start(client);
```

Managing indices
----------------

When Beyonder starts, it tries to find index names and settings in the classpath.

If you add in your classpath a file named `elasticsearch/twitter`, the `twitter` index will be automatically created
at startup if it does not exist yet.

If you add in your classpath a file named `elasticsearch/twitter/_settings.json`, it will be automatically applied to define
settings for your `twitter` index.

For example, create the following file `src/main/resources/elasticsearch/twitter/_settings.json` in your project:

```javascript
{
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 2
  },
  "mappings": {
    "properties": {
      "message": {
        "type": "text"
      }
    }
  }
}
```

By default, Beyonder will not overwrite an index if it already exists.
This can be overridden by setting `force` to `true` in the expanded factory method
`ElasticsearchBeyonder.start()`.

Managing templates
------------------

Sometimes it's useful to define a template mapping that will automatically be applied to new indices created. 

For example, if you planned to have indexes per year for twitter feeds (twitter2012, twitter2013, twitter2014) and you want
to define a template named `twitter_template`, you can add a file named `elasticsearch/_template/twitter_template.json`
in your project:

```javascript
{
    "template" : "twitter*",
    "settings" : {
        "number_of_shards" : 1
    },
    "mappings" : {
        "properties" : {
            "message" : {
                "type" : "text"
            }
        }
    }
}
```

Managing pipelines
------------------

A pipeline is a definition of a series of processors that are to be executed in the same order as they are declared while 
documents are being indexed. Please note that this feature is only supported when u use the REST client not the Transport client.

For example, setting one fields value based on another field by using an Set Processor you an add a file named `elasticseasrch/_pipeline/set_field_processor`
in your project:

```javascript
{
  "description" : "Twitter pipeline",
  "processors" : [
    {
      "set" : {
        "field": "foo",
        "value": "bar"
      }
    }
  ]
}
```

By default, Beyonder will not overwrite a pipeline if it already exists.
This can be overridden by setting `force` to `true` in the expanded factory method
`ElasticsearchBeyonder.start()`.

Why this name?
==============

I was actually looking for a cool name in the marvel characters list and found
that [Beyonder](http://marvel.wikia.com/Beyonder_(Earth-616)) was actually a very
powerful character.

This project gives some features beyond elasticsearch itself. :)

# Tests

This project comes with unit tests and integration tests.
You can disable running them by using `skipTests` option as follows:

```sh
mvn clean install -DskipTests
```

## Unit Tests

If you want to disable only running unit tests, use `skipUnitTests` option:

```sh
mvn clean install -DskipUnitTests
```

## Integration Tests

Integration tests are launching a Docker instance. So you need to have Docker installed.

If you want to disable running integration tests, use `skipIntegTests` option:

```sh
mvn clean install -DskipIntegTests
```

If you wish to run integration tests against a cluster which is already running externally, you can configure the
following settings to locate your cluster:

|            setting            |          default        |
|:-----------------------------:|:-----------------------:|
| `tests.cluster`               | `http://127.0.0.1:9400` |
| `tests.cluster.transport.port`| `9500`                  |

For example:

```sh
mvn clean install -Dtests.cluster=http://127.0.0.1:9200 -Dtests.cluster.transport.port=9300
```

If you want to run your tests against an [Elastic Cloud](https://cloud.elastic.co/) instance, you can use something like:

```sh
mvn clean install \
    -Dtests.cluster=https://CLUSTERID.eu-west-1.aws.found.io:9243 \
    -Dtests.cluster.user=elastic \
    -Dtests.cluster.pass=GENERATEDPASSWORD
```

When user and password are set only Rest Tests are ran.

License
=======

This software is licensed under the Apache 2 license, quoted below.

	Copyright 2011-2020 David Pilato
	
	Licensed under the Apache License, Version 2.0 (the "License"); you may not
	use this file except in compliance with the License. You may obtain a copy of
	the License at
	
	    http://www.apache.org/licenses/LICENSE-2.0
	
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
	WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
	License for the specific language governing permissions and limitations under
	the License.
