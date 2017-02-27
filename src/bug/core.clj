(ns bug.core)

(def the-map {:a 2})

(defn related? [protocol x]
  {:extends? (extends? protocol (type x))
   :satisfies? (satisfies? protocol x)}
  )



