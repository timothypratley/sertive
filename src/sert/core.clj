(ns sert.core
  "Recommendations on writting assertions in Clojure.
  Motivation: http://research.microsoft.com/pubs/70290/tr-2006-54.pdf
  This namespace is intended to be evaluated interactively at a REPL."
  (:require [clojure.test :refer [is do-report]]))

(comment "Compose is with assert. is prints expected/actual and returns success or failure.")
(assert (is (= 1 2)))
(assert (is (string? 1)))

(comment "Compare with (assert (string? s))")

(comment "Use preconditions to check arguments, compose with is.")
(defn foo [^String s]
  {:pre [(is (string? s))]}
  (str "hi " s))
(foo 1) ; shows you the values

(comment "Throwing InvalidArgument exceptions is a fine alternative to asserting. 'must be a string' is not enough information, report what was passed in.")
(defn my-str [x]
  (when (not (string? x))
    (throw (ex-info "Illegal argument" {:x x}))))
(my-str 1)
(comment "or")
(defn my-str2 [x]
  (when (not (string? x))
    (throw (IllegalArgumentException. (str x " is not a string")))))
(my-str2 1)
(comment "I prefer preconditions as they are symantic and concise.")

(comment "Compose with not for negative assertions.")
(assert (is (not (= 1 1))))

(comment "Sometimes it is useful to call a function on a value, but return the value. The way to do this is doto.")
(doto 1 println) ; prints 1 and returns 1
(doto {:foo 1 :bar 2} clojure.pprint/pprint)
(comment "spy is still useful when you want to see the actual form. If you find yourself using spy, chances are that the docs and assertions surrounding the code or the caller are insufficient, consider updating them once you find the issue.")


(doto 1 assert) ; asserts 1 and returns 1
(comment "is returns the value, so can be composed with assert and doto.")
(doto (is 1) assert)

(comment "Be careful though, as doto is a macro similar to ->, below does not do what you want.")
(doto (inc 1) #(assert (is (string? %))))
(comment "This creates and discards a function, it does not assert. Don't do this.")

(comment "However you can reorder the expression with as->")
(doto (inc 1) (as-> x (assert (is (string? x)))))
(comment "This works fine.")

(comment "It may be useful to create a macro that provides this capability without resorting to as->.")
(defmacro check
  "Like (assert (is form)) when (pred form) is false
  the value of the form if the predicate is true."
  [form pred msg]
  `(let [result# ~form
         check# (~pred result#)
         expected# (list '~pred '~form)]
     (if check#
       result#
       (do (do-report {:type :fail
                           :message ~msg
                           :expected expected#
                           :actual (list '~'not (list '~pred result#))})
           (assert check# (str expected#))))))

(check (str (inc 1)) string? "abc") ; returns "2"

(comment "I can also imagine arity versions that don't require msg or pred for convenience.")
