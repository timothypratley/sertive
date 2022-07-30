.PHONY: all test update

all: test

test:
	clojure -M:test

update:
	clojure -M:outdated
