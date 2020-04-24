import { store } from "@risingstack/react-easy-state";

class ErrorContext {
  constructor(message, context) {
    this.message = message;
    this.context = context;
  }
}

const state = store({
  errorStore: {
    error: null,
  },
  showStore: {
    shows: {},
    isLoading: false,
    async fetchShows() {
      state.showStore.isLoading = true;
      await fetch("/api/v1/source/")
        .then((response) => {
          state.showStore.isLoading = false;
          if (!response.ok) {
            throw Error(response.statusText + " " + response.status);
          }
          return response.json();
        })
        .then((shows) => {
          state.showStore.shows = shows.reduce(function (map, obj) {
            map[obj.id] = obj;
            return map;
          }, {});

          state.errorStore.error = null;
        })
        .catch(function (error) {
          state.errorStore.error = new ErrorContext(
            error.message,
            "fetching shows from the server"
          );
        });
    },
  },
  searchStore: {
    query: "",
    queryForLatestResults: "",
    isLoading: false,
    queryResults: null,
    async executeQuery() {
      // ignore empty query
      if (
        state.searchStore.query == null ||
        state.searchStore.query.replace(/^\s+/, "").replace(/\s+$/, "") === ""
      ) {
        return;
      }
      state.searchStore.isLoading = true;
      // if the results contain the currently selected media it will auto-play
      // unless the selection is set to null explicitly on a new query
      state.playerStore.selected = null;
      await fetch(
        "/api/v1/query/" + encodeURIComponent(state.searchStore.query)
      )
        .then((response) => {
          state.searchStore.isLoading = false;
          if (!response.ok) {
            throw Error(response.statusText + " " + response.status);
          }
          return response.json();
        })
        .then((results) => {
          console.log(results);
          state.searchStore.queryForLatestResults = state.searchStore.query;
          state.searchStore.queryResults = results.aggregatedByMedia;
          state.errorStore.error = null;
        })
        .catch(function (error) {
          state.errorStore.error = new ErrorContext(
            error.message,
            "searching for content"
          );
        });
    },
  },
  playerStore: {
    selected: null,
    toggleSelection(index) {
      if (state.playerStore.isSelected(index)) {
        state.playerStore.selected = null;
      } else {
        state.playerStore.selected = index.mediaId + "_" + index.offset;
      }
      console.log(state.playerStore);
    },
    isSelected(index) {
      return state.playerStore.selected === index.mediaId + "_" + index.offset;
    },
  },
});

export { state };
