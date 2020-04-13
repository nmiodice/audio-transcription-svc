import React from 'react';
import './App.css';
import ShowList from './ShowList';

import { Header } from './Header2';
import ErrorAlert from './components/ErrorAlert';
import { view } from '@risingstack/react-easy-state';

import {state} from './state/state'

window.state = state

state.showStore.fetchShows()

class App extends React.Component {
  render() {
    return (
      <div className="App">
        <Header></Header>
        <ErrorAlert error={state.errorStore.error}></ErrorAlert>

        {/* <ShowList></ShowList> */}

        {/* <Header/>
        <header className="App-header">
          <img src={logo} className="App-logo" alt="logo" />
          <p>
            Edit <code>src/App.js</code> and save to reload.
          </p>
          <a
            className="App-link"
            href="https://reactjs.org"
            target="_blank"
            rel="noopener noreferrer"
          >
            Learn React
          </a>
        </header> */}
      </div>
    );
  }
}

export default view(App);
