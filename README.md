# Build
```
sudo docker build -t versions-replacer .
```

# Run
```
sudo docker run --rm versions-replacer --help
sudo docker run -v /files:/files --rm versions-replacer /files/txtfile /files/ymlfile
```

> Without docker (requires Java 11 or higher):
> ```
> java -jar artifacts/versions-replacer-1.0.jar --help
> java -jar artifacts/versions-replacer-1.0.jar /path/to/txt/file /path/to/yaml/file
> ```
