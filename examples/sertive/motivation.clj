(ns sertive.motivation
  "My misguided adventures in error reporting.
  This namespace is intended to be evaluated interactively at a REPL."
  (:require [clojure.test :refer [is do-report]]
            [clojure.pprint :refer [pprint]]))

(def i 2)

(assert (string? i))
;; CompilerException java.lang.AssertionError: Assert failed: (string? i)
;;; Bad: does not reveal what i is.

;; Composing assert with is increases input visibility:
(assert (is (string? i)))
;; FAIL in clojure.lang.PersistentList$EmptyList@1 (sert.clj:12)
;; expected: (string? i)
;; actual: (not (string? 2))
;; CompilerException java.lang.AssertionError: Assert failed: (is (string? i))
;;; Good: Tells me what was not a string in a familiar form.
;;; Bad: clojure.lang.PersistentList$EmptyList@1 is a distraction,
;;; (clojure.test/is should not print empty *testing-vars*)
;; Bad: Negative assertions should preserve their form.
(assert (is (not (number? i))))
;; actual: (not (not true))
;; Bad: What is i?
;; Cause: clojure.test/assert-predicate does not work with macros
;; and is not recursive (nested functions also don't work).

(assert (string? i) i)
;; Assert failed: 2 (string? i)
;;; Good: I can see what i is.
;;; Bad: A little cryptic to write and read.

(defn greet [s]
  {:pre [(string? s)]}
  (str "hi " s))
(greet i)
;; Assert failed: (string? s)
;;; Bad: No idea what i is.
;;; Attention is drawn to greet instead of the call site of greet.
;;; Not explicitly described as preconditions in the error message.

;; Again is can provide input visibility
(defn greet [s]
  {:pre [(is (string? s))]}
  (str "hi " s))
(greet i)
;; FAIL in clojure.lang.PersistentList$EmptyList@1 (sert.clj:29)
;; expected: (string? s)
;; actual: (not (string? 2))
;; CompilerException java.lang.AssertionError: Assert failed: (is (string? s))

;; Throwing InvalidArgument exceptions,
;; 'must be a string' is not enough information; report what was passed in.
(defn my-str [x]
  (when (not (string? x))
    (throw (ex-info "Illegal argument x should be a string" {:x x}))))
(my-str i)
;; Unhandled clojure.lang.ExceptionInfo
;; Illegal argument x should be a string
;; {:x 2}
;;; Good: Symantic exception
;;; Exceptions thrown with ex-info allow programmatic handling
(try
  (throw (ex-info "Argh!" {:x i}))
  (catch Exception e
    (ex-data e)))
;; {:x 2}
;;; Bad: manually capturing vars into a map

;; Base java exceptions
(defn my-str2 [x]
  (when (not (string? x))
    (throw (IllegalArgumentException. (str x " is not a string")))))
;;; Bad: Some friction when conveying environment state.

;; Sometimes it is useful to call a function on a value,
;; but return the value, not the function result.
(doto 1 prn) ; prints 1 and returns 1
(doto 1 (prn "***")) ; prints 1 with trailing stars to stand out
;; Prefer prn over println to prints values, so you can see quoted strings
(prn {:a "foo"})                          ; {:a "foo"}
;; prn is easy to search for and delete later.
(doto (repeatedly 10 rand) pprint)
;; Bad: spy macro is better, but not part of core.

(doto i assert) ; asserts 2 and returns 2
;; is returns its argument, so can be composed with assert and doto.
(doto (is i) assert)
;; Caution! doto is a macro similar to ->
(doto (inc i) #(assert (is (string? %))))
;; Creates and discards a function. It does not assert. Do not do this!
;;; Bad: doto is not expressive enough for use with assert is predicate.

;; You can reorder the expression with as->
(doto (inc i) (as-> x (assert (is (string? x)))))
;;; Bad: this has some cognitive overhead.

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
;; Bad: Still ugly output for lambdas!!
;; FAIL in clojure.lang.PersistentList$EmptyList@1 (form-init115829856417485376.clj:1)
;; expected: ((fn* [p1__4195#] (is (not (number? p1__4195#)))) i)
;; actual: (not ((fn* [p1__4195#] (is (not (number? p1__4195#)))) 2))

;; TODO: Use a dynamicly bound function to handle errors
;; * default to throwing an exception
;; * allow rebinding to do something else
;; * shadow the local with the result of the rebound function?


(defn then [x pred f]
  (if (pred x)
    (f x)
    x))

(then (+ 2 3) odd? mod)

;; collect examples
