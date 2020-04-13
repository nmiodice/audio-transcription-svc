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
        isLoading: false,
        selectedShow: null,
        async fetchShows() {
            state.showStore.isLoading = true
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
                    state.showStore.isLoading = false
                }).catch(function(error) {
                    state.errorStore.error = new ErrorContext(error.message, "fetching shows from the server")
                    state.showStore.isLoading = false
                });
        },
    }
});

export { state }