(ns sertive.core
  "Standardize ex-data to supply when creating an ex-info Exception"
  (:require [sc.impl]
            [clojure.string :as str]
            [sertive.peek :as peek]))

(defn str-term [env term]
  (cond (symbol? term) (str term "=" (peek/strn (get env term) 30))
        (string? term) term
        :else (peek/strn term 30)))

;; TODO: come up with a better heuristic for the term limit size,
;; perhaps based on the number of terms, or just pass the whole thing to strn and let it figure it out
(defn explain-clause [env explain]
  (str ": "
       (str/join " " (for [term explain]
                       (str-term env term)))))

(defn format-msg [id {:keys [tags explain suggest] :as options}]
  (let [env (dissoc options :tags :explain :suggest)
        explain (or explain (keys env))]
    (str "ERROR" id
         (when tags
           (str " [" (str/join "," tags) "]"))
         (when explain
           (explain-clause env explain))
         (when suggest
           (str " (" suggest ")")))))

;; TODO: tags/explain/suggest should be specified externally as an annotation
(defn ex
  "Creates an ex-info.

  Must be provided a fully qualified, unique keyword as the id.
  May specify options as a map:
  {:tags #{:sertive.error/temporarily-unavailable}      ;; refers to a hierarchy of error classifications
   :explain [sym1 \"was even\"] ;; problem encountered, symbols are looked up in env and data
   :suggest \"retry\"           ;; possible automatic resolution
   sym1 <value>                 ;; captures a variable sym/value
   <any-other-key> <value>}     ;; arbitrary data, avoid collisions with reserved option keywords
  May specify a cause exception.

  Has the same arity and behavior as ex-info, but id based instead of message based.
  The message will be generated from the id and reserved optional key/values when present."
  ([id] (ex id {} nil))
  ([id options] (ex id options nil))
  ([id options cause]
   ;; TODO: would like to automatically capture function parameter bindings...
   (let [auto-env {} #_(sc.impl/extract-local-names)
         data (merge auto-env options {:id id})
         msg (format-msg id data)]
     (ex-info msg data cause))))

(defmacro fail
  ([id] `(fail ~id {}))
  ([id options] `(fail ~id ~options)
   `(throw (ex ~id ~options))))

(defmacro catch-and-release [id & body]
  `(try
     ~@body
     (catch #?(:clj Throwable :cljs :default) e
       (throw (ex id {} e)))))
