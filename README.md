Elasticsearch Beyonder
======================

Welcome to the [Elasticsearch](http://www.elastic.co/) Beyonder project.

This project comes historically from [spring-elasticsearch](https://github.com/dadoonet/spring-elasticsearch) project.


Versions
========

| elasticsearch-beyonder  | elasticsearch | Release date |
|:-----------------------:|:-------------:|:------------:|
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
  <version>2.0.0</version>
</dependency>
```

If you want to set a specific version of elasticsearch, add it to your `pom.xml` file:

```xml
<dependency>
  <groupId>org.elasticsearch</groupId>
  <artifactId>elasticsearch</artifactId>
  <version>2.0.0-rc1</version>
</dependency>
```

Adding Beyonder to your client
------------------------------

For both TransportClient and NodeClient, you can define many properties to manage automatic creation
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

License
=======

This software is licensed under the Apache 2 license, quoted below.

	Copyright 2011-2015 David Pilato
	
	Licensed under the Apache License, Version 2.0 (the "License"); you may not
	use this file except in compliance with the License. You may obtain a copy of
	the License at
	
	    http://www.apache.org/licenses/LICENSE-2.0
	
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
	WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
	License for the specific language governing permissions and limitations under
	the License.
