#!/usr/bin/env bash
# Guaranteed-working runner (bypasses any IntelliJ classpath issue).
# Usage: ./run.sh
set -e
cd "$(dirname "$0")"

M2="$HOME/.m2/repository"
SQLITE="$M2/org/xerial/sqlite-jdbc/3.46.1.0/sqlite-jdbc-3.46.1.0.jar"
SLF4J="$M2/org/slf4j/slf4j-api/2.0.13/slf4j-api-2.0.13.jar:$M2/org/slf4j/slf4j-nop/2.0.13/slf4j-nop-2.0.13.jar"
OUT="out/manual-build"

mkdir -p "$OUT"
find src/main/java -name "*.java" > "$OUT/sources.txt"
javac -d "$OUT" -cp "$SQLITE:$SLF4J" @"$OUT/sources.txt"
java -cp "$OUT:src/main/resources:$SQLITE:$SLF4J" com.resala.Main
