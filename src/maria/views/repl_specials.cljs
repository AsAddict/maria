(ns maria.views.repl-specials
  (:require [re-view.core :as v :refer [defview]]
            [maria.ns-utils :as ns-utils]
            [maria.editor :as editor]
            [re-view-material.icons :as icons]
            [clojure.string :as string]
            [cljs.pprint :refer [pprint]]
            [cljs-live.compiler :as c]
            [cljs.tools.reader :as r]
            [cljs.tools.reader.reader-types :as rt]
            [maria.views.repl-utils :as repl-ui])
  (:import goog.string.StringBuffer))

(defn docs-link [namespace name]
  (when (re-find #"^(cljs|clojure)\.core(\$macros)?$" namespace)
    [:.mv2
     [:a.f7.black {:href   (str "https://clojuredocs.org/clojure.core/" name)
                   :target "_blank"
                   :rel    "noopener noreferrer"} "clojuredocs ↗"]]))

(defview doc
  {:life/initial-state #(:expanded? %)
   :key                :name}
  [{:keys [doc
           meta
           arglists
           view/state
           view/props
           standalone?] :as this}]
  (let [[namespace name] [(namespace (:name this)) (name (:name this))]]
    [:.ph3.bt.b--near-white.ws-normal
     {:class (when standalone? repl-ui/card-classes)}
     [:.code.flex.items-center.pointer.mv1
      {:on-click #(swap! state not)}
      (when standalone?
        [:span.o-60 namespace "/"])
      name
      [:.flex-auto]
      [:span.o-50
       (if @state icons/ArrowDropUp
                  icons/ArrowDropDown)]]
     (when @state
       (list
         [:.mv1.blue (string/join ", " (map str (ns-utils/elide-quote (or (:arglists meta) arglists))))]
         [:.gray.mv2 doc]
         (docs-link namespace name)))]))

(defn source-of-form-at-position [source line column]
  (let [reader (rt/SourceLoggingPushbackReader.
                 (rt/string-push-back-reader source 1)
                 line
                 column
                 true
                 nil
                 0
                 "temp-clj"
                 (atom {:buffer (StringBuffer.) :offset '(0)}))
        form (r/read reader)]
    (:source (meta form))))

(defview source
  [{:keys [meta line column]}]
  (repl-ui/card
    (if-let [source (get @c/cljs-cache (:file meta))]
      (editor/viewer (source-of-form-at-position source line column))
      [:.pa2 "Source not found"])))

(defview dir
  {:life/initial-state {:expanded? false}}
  [{:keys [view/state]} c-state ns]
  (let [defs (->> (:defs (ns-utils/analyzer-ns @c-state ns))
                  (seq)
                  (sort)
                  (map second)
                  (filter #(not (:private (:meta %)))))
        c (count defs)
        limit 10
        {:keys [expanded?]} @state]
    [:.sans-serif
     {:class repl-ui/card-classes}
     [:.b.pv2.ph3.f5 (str ns)]
     (map doc (cond->> defs
                       (not expanded?) (take limit)))
     (when (and (not expanded?) (> c limit))
       [:.o-50.pv2.flex.items-center.ph3.pointer.bt.b--near-white
        {:on-click #(swap! state assoc :expanded? true)}
        [:span "Show All (" (- c limit) " more)"]
        [:.flex-auto] icons/ExpandMore])]))

