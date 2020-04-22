import React from "react";
import "./App.css";

import Header from "./components/Header";
import SearchBar from "./components/SearchBar";
import ErrorAlert from "./components/ErrorAlert";
import Results from "./components/Results";

import { view } from "@risingstack/react-easy-state";
import { state } from "./state/state";

import { loadAppInsights } from './telemetry'

const appInsights = loadAppInsights()
appInsights.trackPageView();

class App extends React.Component {
  render() {
    return (
      <div className="App">
        <Header
          title="Podcast Search"
        />
        <ErrorAlert error={state.errorStore.error} />
        <SearchBar
          query={state.searchStore.query}
          queryResults={state.searchStore.queryResults}
          placeholder="Search for your favorite podcast content..."
        />
        <Results
          queryIsLoading={state.searchStore.isLoading}
          queryResults={state.searchStore.queryResults}
          query={state.searchStore.queryForLatestResults}
        />
      </div>
    );
  }
}

export default view(App);
