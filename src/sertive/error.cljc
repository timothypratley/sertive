(ns sertive.error
  "A standard hierarchy of error classifications which can be extended by users")

(def tags
  (-> (make-hierarchy)
    (derive ::temporarily-unavailable ::io)
    (derive ::not-found ::io)
    (derive ::is-not ::expectation)
    (derive ::greater-than ::expectation)
    (derive ::less-than ::expectation)
    (derive ::equal ::expectation)
    (derive ::not-equal ::expectation)
    (derive ::matches-regex ::expectation)
    (derive ::does-not-match-regex ::expectation)))
