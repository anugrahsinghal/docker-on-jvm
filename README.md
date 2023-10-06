[![progress-banner](https://app.codecrafters.io/progress/docker/0beab38c-3abb-4974-a4ca-4c4d9de3784f)](https://app.codecrafters.io/users/anugrahsinghal)

This is a starting point for Java solutions to the
["Build Your Own Docker" Challenge](https://codecrafters.io/challenges/docker).

In this challenge, you'll build a program that can pull an image from
[Docker Hub](https://hub.docker.com/) and execute commands in it. Along the way,
we'll learn about [chroot](https://en.wikipedia.org/wiki/Chroot),
[kernel namespaces](https://en.wikipedia.org/wiki/Linux_namespaces), the
[docker registry API](https://docs.docker.com/registry/spec/api/) and much more.

**Note**: If you're viewing this repo on GitHub, head over to
[codecrafters.io](https://codecrafters.io) to try the challenge.

# How to Run

Note: This section is for stages 2 and beyond.

You'll use linux-specific syscalls in this challenge. so we'll run your code
_inside_ a Docker container.

Please ensure you have [Docker installed](https://docs.docker.com/get-docker/)
locally.

Next, add a [shell alias](https://shapeshed.com/unix-alias/):

```sh
alias mydocker='docker build -t mydocker . && docker run --cap-add="SYS_ADMIN" mydocker'
```

(The `--cap-add="SYS_ADMIN"` flag is required to create
[PID Namespaces](https://man7.org/linux/man-pages/man7/pid_namespaces.7.html))

You can now execute your program like this:

```sh
mydocker run ubuntu:latest echo hey
```