(ns matcher.matchers
  (:refer-clojure :exclude [list])
  (:require [clojure.core.unify :as u]
            [matcher.utils.match :as utils]))

(defn list [ & rest]
  (fn [m]
    (when (list? m)
      (let [[before after] (split-with #(not= % '&) rest)
            m-size (count m)
            b-size (count before)
            b-vec (vec before)
            m-vec (vec m)]
        (cond
          (empty? after) (when (= m-size b-size) [b-vec m-vec])
          (= 2 (count after)) (when (>= m-size b-size)
                                [(conj b-vec (last after))
                                 (conj (subvec m-vec 0 b-size) (drop b-size m-vec))])
          :else nil)))))

(defmacro match [obj & matches]
  (apply utils/match* obj matches))
