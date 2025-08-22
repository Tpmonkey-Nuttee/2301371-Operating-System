seq 1 20 | xargs -n1 -P10  curl "http://localhost:8080/"
echo "DONE"
