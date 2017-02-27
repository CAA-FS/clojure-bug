(ns bug.core)

(def the-map {:a 2})

(defn related? [protocol x]
  {:extends? (extends? protocol (type x))
   :satisfies? (satisfies? protocol x)}
  )

;; Now eval this:

(related? clojure.core.protocols/IKVReduce the-map)

;; Problem is that Clojure 1.7 gives:

{:extends? true, :satisfies? true}

;; but Clojure 1.8 gives:

{:extends? false, :satisfies? true}

;; What gives?
