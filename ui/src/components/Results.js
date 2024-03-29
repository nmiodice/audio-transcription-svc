import React from "react";
import { makeStyles } from "@material-ui/core/styles";
import { view } from "@risingstack/react-easy-state";

import Result from "./Result";
import CircularProgress from '@material-ui/core/CircularProgress';


const useStyles = makeStyles((theme) => ({
  root: {
    height: "100%",
    width: "100%",
  },
  results: {
    position: "absolute",
    top: "20%",
    height: "75%",
    left: "50%",
    width: "80%",
    transform: "translate(-50%)",
    minWidth: "400px",
  },
  loading: {
    margin: '40%',
    height: '80px',
    width: '80px',
  },
}));

export default view(function Results(props) {
  const classes = useStyles();

  if (props.queryResults == null && !props.queryIsLoading) {
    return null;
  }

  if (props.queryIsLoading) {
    return (
      <div className={classes.root}>
        <CircularProgress className={classes.loading}/>
      </div>
    )
  }

  if (Object.keys(props.queryResults).length === 0) {
    return (
      <div className={classes.root}>
        <div className={classes.results}>
          <p>Oops! No results...</p>
          <br />
          <div className={classes.center}>¯\_(ツ)_/¯</div>
        </div>
      </div>
    );
  }
  return (
    <div className={classes.root}>
      <div className={classes.results}>
        {Object.keys(props.queryResults).map(function (showId) {
          return (
            <Result
              key={showId}
              result={props.queryResults[showId]}
              query={props.query}
            />
          );
        })}
      </div>
    </div>
  );
});
