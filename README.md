Elasticsearch Beyonder
======================

Welcome to the [Elasticsearch](http://www.elastic.co/) Beyonder project.

This project comes historically from [spring-elasticsearch](https://github.com/dadoonet/spring-elasticsearch) project.


Versions
========

| elasticsearch-beyonder  | elasticsearch | Release date |
|:-----------------------:|:-------------:|:------------:|
| 5.2-SNAPSHOT            | 5.x, 6.x      |              |
| 5.1                     | 5.x, 6.x      |  2017-07-12  |
| 5.0                     | 5.x, 6.x      |  2017-07-11  |
| 2.1.0                   | 2.0, 2.1      |  2015-11-25  |
| 2.0.0                   |      2.0      |  2015-10-24  |
| 1.5.0                   |      1.5      |  2015-03-27  |
| 1.4.1                   |      1.4      |  2015-03-02  |
| 1.4.0                   |      1.4      |  2015-02-27  |


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
  <version>5.1</version>
</dependency>
```

You need to import as well the elasticsearch client you want to use by adding one of the following
dependencies to your `pom.xml` file.

For example, here is how to import the REST Client to your project:

```xml
<dependency>
    <groupId>org.elasticsearch.client</groupId>
    <artifactId>elasticsearch-rest-client</artifactId>
    <version>5.6.7</version>
</dependency>
```

For example, here is how to import the Transport Client to your project (deprecated):

```xml
<dependency>
    <groupId>org.elasticsearch.client</groupId>
    <artifactId>transport</artifactId>
    <version>5.6.7</version>
</dependency>
```

For example, here is how to import the Secured Transport Client to your project (deprecated):

```xml
<dependency>
    <groupId>org.elasticsearch.client</groupId>
    <artifactId>x-pack-transport</artifactId>
    <version>5.6.7</version>
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

Elasticsearch now provides a [Rest Client](https://www.elastic.co/guide/en/elasticsearch/client/java-rest/5.4/index.html).
It's the recommended way as the Transport Client is now deprecated and will be removed in a next major version.

Just pass to Beyonder a Rest Client instance:

```java
RestClient client = RestClient.builder(new HttpHost("127.0.0.1", 9200)).build();
ElasticsearchBeyonder.start(client);
```

For the record, you can also use X-Pack security with:

```java
CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("elastic", "changeme"));
RestClient client = RestClient.builder(new HttpHost("127.0.0.1", 9200))
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

## Using Secured Transport Client (deprecated)

To use the deprecated TransportClient, just pass it to Beyonder:

```java
Client client = new PreBuiltXPackTransportClient(Settings.builder().put("xpack.security.user", "elastic:changeme").build())
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
  "index" : {
    "number_of_shards" : 3,
    "number_of_replicas" : 2
  }
}
```

Managing types
--------------

If you define a file named `elasticsearch/twitter/tweet.json`, it will be automatically applied as the mapping for
the `tweet` type in the `twitter` index.

For example, create the following file `src/main/resources/elasticsearch/twitter/tweet.json` in your project:

```javascript
{
  "tweet" : {
    "properties" : {
      "message" : {"type" : "string", "store" : "yes"}
    }
  }
}
```

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
        "tweet" : {
            "properties" : {
                "message" : {
                    "type" : "string",
                    "store" : "yes"
                }
            }
        }
    }
}
```


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

Integration tests are launching an elasticsearch server thank to [elasticsearch-maven-plugin](https://github.com/alexcojocaru/elasticsearch-maven-plugin/)
which makes that super easy to have!

Tests are ran by default on the 5.x series but you can choose to run them against another version by using the following
maven profiles:

* `es-6x`: runs against an elasticsearch 6.x cluster
* `es-5x` (default): runs against an elasticsearch 5.x cluster


If you want to disable running integration tests, use `skipIntegTests` option:

```sh
mvn clean install -DskipIntegTests
```

Integration tests can be ran against a cluster secured by [x-pack](https://www.elastic.co/downloads/x-pack).
You have to activate as well the `es-xpack` profile:

```sh
mvn clean install -Pes-xpack
```

Or if you want to run x-pack with a given elasticsearch version:

```sh
mvn clean install -Pes-6x -Pes-xpack
```

If you wish to run integration tests against a cluster which is already running externally, you can configure the
following settings to locate your cluster:

|            setting            |     default   |
|:-----------------------------:|:-------------:|
| `tests.cluster.host`          | `127.0.0.1`   |
| `tests.cluster.scheme`        | `http`        |
| `tests.cluster.rest.port`     | `9400`        |
| `tests.cluster.transport.port`| `9500`        |
| `tests.cluster.user`          | `elastic`     |
| `tests.cluster.pass`          | `changeme`    |

For example:

```sh
mvn clean install -Dtests.cluster.rest.port=9200 -Dtests.cluster.transport.port=9300
```

If you want to run your tests against an [Elastic Cloud](https://cloud.elastic.co/) instance, you can use something like:

```sh
mvn clean install \
    -Dtests.cluster.host=CLUSTERID.eu-west-1.aws.found.io \
    -Dtests.cluster.scheme=https \
    -Dtests.cluster.rest.port=9243 \
    -Dtests.cluster.transport.port=9300 \
    -Dtests.cluster.user=elastic \
    -Dtests.cluster.pass=GENERATEDPASSWORD
```

License
=======

This software is licensed under the Apache 2 license, quoted below.

	Copyright 2011-2017 David Pilato
	
	Licensed under the Apache License, Version 2.0 (the "License"); you may not
	use this file except in compliance with the License. You may obtain a copy of
	the License at
	
	    http://www.apache.org/licenses/LICENSE-2.0
	
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
	WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
	License for the specific language governing permissions and limitations under
	the License.
