(ns sertive.peek
  "Summarize values to fit inside a character limit")

(declare strn)

(defn strn-coll
  "For non-map collections (sets, vectors, lists)
  mutually recurs with strn on values within."
  [coll n]
  {:pre [(or (set? coll)
             (vector? coll)
             (seq? coll))
         (>= n 6)]}
  (let [inner-length (if (set? coll)
                       (- n 6)
                       (- n 5))
        vs (loop [acc nil
                  [head & tail] coll]
             (let [acc' (str acc (strn head 10) " ")]
               (if (<= (count acc') inner-length)
                 (recur acc' tail)
                 (str acc "..."))))]
    (str (cond (set? coll) "#{"
               (vector? coll) "["
               :else "(")
         ;; TODO: could take more elements if they fit in the bud
         vs
         (cond (set? coll) "#}"
               (vector? coll) "]"
               :else ")"))))

(defn strn-map
  [m n]
  (let [s (pr-str m)]
    (if (<= (count s) n)
      s
      ;; TODO: need to do better math here... (- n 5) might be less than 6 (required for strn-coll
      (let [s (str "{..." (strn-coll (keys m) (- n 5)) "}")]
        (if (<= (count s) n)
          s
          ;; TODO: should coll-str show counts instead/as well?
          (let [s (str "{..." (count m) "}")]
            (if (<= (count s) n)
              s
              "{...}")))))))

(defn strn-any [any n]
  (let [s (pr-str any)]
    (if (<= (count s) n)
      s
      (str (subs s 0 (- n 3)) "..."))))

(defn strn
  "Like pr-str but constrains result length <= n"
  [x n]
  {:pre [(> n 5)]}
  (cond (map? x) (strn-map x n)
        (coll? x) (strn-coll x n)
        :else (strn-any x n)))
