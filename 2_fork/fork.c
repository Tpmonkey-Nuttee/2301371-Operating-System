#include <sys/types.h>
#include <stdio.h>
#include <unistd.h>
#include <sys/wait.h>
#include <stdlib.h>


int main() {
	pid_t pids[10];
	pid_t currentPid = getpid();

	for (size_t i = 0; i < 10; i++) {
		if ((pids[i] = fork()) < 0) {
			printf("Fork failed\n");
			return 1;
		} else if (pids[i] == 0) {
			currentPid = getpid();
			printf("I'm the child numer %zu (pid %d)\n", i+1, currentPid);
			exit(0);
		}
	}

	for (size_t i = 0; i < 10; i++) {
		int status;
		wait(&status); // magic fuck 2
	}
	printf("Parent terminated (pid %d)\n", currentPid);

	return 0;
}
