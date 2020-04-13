import React from 'react';
import './App.css';

import Header from './components/Header';
import SearchBar from './components/SearchBar';
import ErrorAlert from './components/ErrorAlert';

import { view } from '@risingstack/react-easy-state';
import {state} from './state/state'



window.state = state

state.showStore.fetchShows()

class App extends React.Component {
  render() {
    return (
      <div className="App">
        <Header title="Audio Search" github_link="https://github.com/nmiodice/audio-transcription-svc/"/>
        <ErrorAlert error={state.errorStore.error}></ErrorAlert>
        <SearchBar placeholder="Search podcasts..."></SearchBar>
      </div>
    );
  }
}

export default view(App);
