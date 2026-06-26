name: Build SparkDuels

on:
  push:
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven

      - name: Build with Maven
        run: mvn -B package --file pom.xml

      - name: Upload plugin JAR
        uses: actions/upload-artifact@v4
        with:
          name: Spark-Practice
          path: target/Spark-Practice.jar
          if-no-files-found: error
