# Build
```
sudo docker build -t versions-replacer .
```

# Run
```
sudo docker run --rm versions-replacer --help
sudo docker run -v /files:/files --rm versions-replacer /files/txtfile /files/ymlfile
```
Or with helper script:
```
./replacer-docker.sh /path/to/txt/file /path/to/yaml/file
```

> Without docker (requires Java 11 or higher):
> ```
> java -jar artifacts/versions-replacer.jar --help
> java -jar artifacts/versions-replacer.jar /path/to/txt/file /path/to/yaml/file
> ```
