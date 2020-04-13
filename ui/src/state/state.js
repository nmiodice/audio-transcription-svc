import { store } from '@risingstack/react-easy-state';

class ErrorContext {
    constructor(message, context) {
      this.message = message;
      this.context = context;
    }
  }

const state = store({
    errorStore: {
        error: null
    },
    showStore: {
        shows: {},
        selectedShow: null,
        async fetchShows() {
            await fetch('/api/v1/source/')
                .then((response) => {
                    if (!response.ok) {
                        throw Error(response.statusText);
                    }
                    return response.json();
                })
                .then((shows) => {
                    state.showStore.shows = shows.reduce(function(map, obj) {
                        map[obj.id] = obj;
                        return map;
                    }, {})

                    state.errorStore.error = null
                    state.showStore.selectedShow = null
                }).catch(function(error) {
                    state.errorStore.error = new ErrorContext(error.message, "fetching shows from the server")
                });
        },
    }
});

export { state }