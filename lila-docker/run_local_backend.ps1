$env:LILA_DOMAIN="localhost:9663"
$env:LILA_URL="http://localhost:9663"
$env:MOCK_EMAIL="true"
$env:ENABLE_MONITORING="false"
$env:ENABLE_RATE_LIMITING="false"
$env:LILA_REDIS_URI="redis://localhost:6379"
$env:LILA_DB_DUAL_URL="mongodb://localhost:27017,localhost:27017"
$env:HTTP_PORT="9663"

# Important Java options from lila.sh
$env:JAVA_OPTS="-Dreactivemongo.api.bson.document.strict=false -Dplay.http.parser.allowEmptyFiles=false"

Write-Host "Starting Lichess Backend Locally (sbt run)..."
cd repos/lila

# Use cmd /c specifically to ensure sbt.bat is picked up if on Windows path
# Or just sbt if it is an exe.
# sbt usually installs sbt.bat on Windows.
sbt run
