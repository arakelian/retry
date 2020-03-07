[![Build Status](https://travis-ci.org/rhuffman/re-retrying.svg?branch=master)](https://travis-ci.org/rhuffman/re-retrying)
[![Latest Version](http://img.shields.io/badge/latest-3.0.0-brightgreen.svg)](https://github.com/rhuffman/re-retrying/releases/tag/v3.0.0-rc.1)
[![License](http://img.shields.io/badge/license-apache%202-brightgreen.svg)](https://github.com/rhuffman/re-retrying/blob/master/LICENSE)

## What is this?

The retry module provides a general purpose method for retrying arbitrary Java code with specific stop, retry, 
and exception handling capabilities that are enhanced by Guava's predicate matching.

This is a fork of the [guava-retrying](https://github.com/rholder/guava-retrying) library by Ryan Holder (rholder), 
which is itself a fork of the [RetryerBuilder](http://code.google.com/p/guava-libraries/issues/detail?id=490) by 
Jean-Baptiste Nizet (JB). The retry project added a Gradle build for pushing it up to Maven Central, and 
exponential and Fibonacci backoff [WaitStrategies](http://rholder.github.io/guava-retrying/javadoc/2.0.0/com/github/rholder/retry/WaitStrategies.html)
that might be useful for situations where more well-behaved service polling is preferred.

## Reasons for Fork

* Add Java 11 support
* Use java.util.Predicate and java.util.Function instead of Guava equivalents
* Make compatible with latest versions of Guava (27+)
* Fix all errorprone warnings in original source code

## Installation

The library is available on [Maven Central](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.arakelian%22%20AND%20a%3A%22retry%22).

### Maven

Add the following to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>central</id>
        <name>Central Repository</name>
        <url>http://repo.maven.apache.org/maven2</url>
        <releases>
            <enabled>true</enabled>
        </releases>
    </repository>
</repositories>

...

<dependency>
    <groupId>com.arakelian</groupId>
    <artifactId>retry</artifactId>
    <version>3.7.0</version>
    <scope>test</scope>
</dependency>
```

### Gradle

Add the following to your `build.gradle`:

```groovy
repositories {
  mavenCentral()
}

dependencies {
  testCompile 'com.arakelian:retry:3.7.0'
}
```

## Licence

Apache Version 2.0

## Documentation
Javadoc can be found [here](http://rholder.github.io/guava-retrying/javadoc/2.0.0).

## License
The retry module is released under version 2.0 of the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).

## Contributors
* Jean-Baptiste Nizet (JB)
* Jason Dunkelberger (dirkraft)
* Diwaker Gupta (diwakergupta)
* Jochen Schalanda (joschi)
* Shajahan Palayil (shasts)
* Olivier Grégoire (fror)
* Andrei Savu (andreisavu)
* (tchdp)
* (squalloser)
* Yaroslav Matveychuk (yaroslavm)
* Stephan Schroevers (Stephan202)
* Chad (voiceinsideyou)
* Kevin Conaway (kevinconaway)
* Alberto Scotto (alb-i986)
* Ryan Holder(rholder)

