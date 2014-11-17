(ns sert
  "Recommendations on writting assertions in Clojure.
  Motivation: http://research.microsoft.com/pubs/70290/tr-2006-54.pdf
  This namespace is intended to be evaluated interactively at a REPL."
  (:require [clojure.test :refer [is do-report]]
            [clojure.pprint :refer [pprint]]
            [slingshot.slingshot :refer [try+ throw+]]))

(def i 2)

;; Compose assert with is for input visibility
(assert (is (string? i)))
;; FAIL in clojure.lang.PersistentList$EmptyList@1 (sert.clj:12)
;; expected: (string? i)
;; actual: (not (string? 2))
;; CompilerException java.lang.AssertionError: Assert failed: (is (string? i))

;; Problem:
;; clojure.lang.PersistentList$EmptyList@1 is a distraction,
;; clojure.test/is should not print empty *testing-vars*.

;; You can optinally pass a message or extra information to assert
(assert (is (string? i)) {:foo "bar"})
;; FAIL in clojure.lang.PersistentList$EmptyList@1 (form-init115829856417485376.clj:1)
;; expected: (string? i)
;; actual: (not (string? 2))
;; AssertionError Assert failed: {:foo "bar"}

;; Compare with
(assert (string? i))
;; CompilerException java.lang.AssertionError: Assert failed: (string? i)
;; which does not reveal what i is.

;; Use preconditions to check arguments. Check with is for input visibility
(defn greet [s]
  {:pre [(is (string? s))]}
  (str "hi " s))
(greet i) ; shows you the values
;; FAIL in clojure.lang.PersistentList$EmptyList@1 (sert.clj:29)
;; expected: (string? s)
;; actual: (not (string? 2))
;; CompilerException java.lang.AssertionError: Assert failed: (is (string? s))

;; Problem:
;; Preconditions should be reported at the caller site,
;; and explicitly described as preconditions in the error message.

;; Throwing InvalidArgument exceptions is a fine alternative.
;; Keep in mind 'must be a string' is not enough information;
;; report what was passed in.
(defn my-str [x]
  (when (not (string? x))
    (throw (ex-info "Illegal argument x should be a string" {:x x}))))
(my-str i)
;; Unhandled clojure.lang.ExceptionInfo
;; Illegal argument x should be a string
;; {:x 2}

;; Problem:
;; There are four options for argument checking
;;   * Typed Clojure
;;   * Schema
;;   * Preconditions
;;   * Throwing an exception
;; All of them have unique pros and cons

;; Exceptions thrown with ex-info allow programmatic handling
(try
  (throw (ex-info "Argh!" {:x i}))
  (catch Exception e
    (ex-data e)))
;; {:x 2}

;; Meaningful java exceptions are acceptable,
;; but there is some friction when conveying environment state.
(defn my-str2 [x]
  (when (not (string? x))
    (throw (IllegalArgumentException. (str x " is not a string")))))
;; Preconditions and assertions are more concise and symantic.

;; Doing some cleanup before throwing an exception may indicated an opportunity
;; to use with-open.
;; The slingshot library has powerful data conveyence and handling features.
(try+
 (throw+ "foo")
 (catch string? e "bar"))

;; Problem:
;; Negative assertions should preserve their form.
(assert (is (not (number? i))))
;; actual: (not (not true))
;; What is i? Please tell me.
;; Cause:
;; clojure.test/assert-predicate only works with functions (not macros)
;; and is not recursive (nested functions also don't work).

;; Sometimes it is useful to call a function on a value, but return the value.
(doto 1 prn) ; prints 1 and returns 1
(doto 1 (prn "***")) ; prints 1 with trailing stars to stand out
;; Prefer prn over println to prints values, so you can see quoted strings
(prn {:a "foo"})                          ; {:a "foo"}
;; prn is easy to search for and delete later.
(doto (repeatedly 10 rand) pprint)
;; Prefer pprint for large values
;; Problem:
;; clojure.repl should provide a spy macro which shows the form and values.

(doto i assert) ; asserts 2 and returns 2
;; is returns its argument, so can be composed with assert and doto.
(doto (is i) assert)
;; Caution! doto is a macro similar to ->
(doto (inc i) #(assert (is (string? %))))
;; Creates and discards a function. It does not assert. Do not do this.
;; Problem:
;; doto macro is not expressive enough for use with assert is predicate.
;; A function version is desirable.

;; You can reorder the expression with as->
(doto (inc i) (as-> x (assert (is (string? x)))))
;; But this has some cognitive overhead.

;; It may be useful to create a macro that provides the same capability:
(defmacro check
  "Like (assert (is form)) when (pred form) is false
  the value of the form if the predicate is true."
  ([form pred] `(check ~form ~pred nil))
  ([form pred msg]
     `(let [result# ~form
            check# (~pred result#)
            expected# (list '~pred '~form)]
        (if check#
          result#
          (let [failure# {:type :fail
                          :message ~msg
                          :expected expected#
                          :actual (list '~'not (list '~pred result#))}]
            (do-report failure#)
            (throw (ex-info (str "Check Failed: " (:actual failure#))
                            failure#)))))))
;; This gives slightly better output
(check (str (inc i)) string?) ; returns "3"
(check (inc i) string?)
;; expected: (string? (inc i))
;; actual: (not (string? 3))
;; ExceptionInfo Check Failed: (not (string? 3))

(check i #(is (not (number? %))))
;; Problem:
;; Still ugly output for lambdas!!
;; FAIL in clojure.lang.PersistentList$EmptyList@1 (form-init115829856417485376.clj:1)
;; expected: ((fn* [p1__4195#] (is (not (number? p1__4195#)))) i)
;; actual: (not ((fn* [p1__4195#] (is (not (number? p1__4195#)))) 2))

;; Functional abstraction is useful.
;; Idiomatic functional composition, namespaced naming and libraries.
