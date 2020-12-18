# OWL2Streams
A benchmark for expressive stream reasoning for dynamic OWL2 Reasoners

OWL2Streams supports three use cases:

- A Smart Building case, requiring the detection COVID-19 risks
- An extension of the OWL2Bench benchmark [1] where the stream consists of students registering to a certain university. 
- An extension of the CityBench benchmark [2], containing more elaborate background knowledge.

# Usage:
```
USAGE: <type>[University|City|Building] <size>[int]
```
For example:
```
java -jar owl2streams.jar University 10
```

This will open 3 entry points:

- url/tbox: allowing to get the ontology TBox
- url/abox: allowing to get the ontology static ABox
- url/event: allowing to pull the next event in the stream

# Compilation
```
mvn clean compile assembly:single
mv target/OWL2Streams-0.0.1-jar-with-dependencies.jar owl2streams.jar
```
