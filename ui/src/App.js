import React from "react";
import "./App.css";

import Header from "./components/Header";
import SearchBar from "./components/SearchBar";
import ErrorAlert from "./components/ErrorAlert";
import Results from "./components/Results";

import { view } from "@risingstack/react-easy-state";
import { state } from "./state/state";

window.state = state;

class App extends React.Component {
  render() {
    return (
      <div className="App">
        <Header
          title="Audio Search"
          github_link="https://github.com/nmiodice/audio-transcription-svc/"
        />
        <ErrorAlert error={state.errorStore.error} />
        <SearchBar
          query={state.searchStore.query}
          queryResults={state.searchStore.queryResults}
          placeholder="Search audio..."
        />
        <Results
          queryResults={state.searchStore.queryResults}
          query={state.searchStore.queryForLatestResults}
        />
      </div>
    );
  }
}

export default view(App);
