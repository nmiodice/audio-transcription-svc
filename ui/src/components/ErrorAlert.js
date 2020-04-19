import React from "react";
import { makeStyles } from "@material-ui/core/styles";
import Alert from "@material-ui/lab/Alert";
import { view } from "@risingstack/react-easy-state";

import Snackbar from "@material-ui/core/Snackbar";

const useStyles = makeStyles((theme) => ({
  root: {
    width: "100%",
  },
}));

export default view(function ErrorAlert(props) {
  const classes = useStyles();

  if (props == null || props.error == null) {
    return null;
  }

  return (
    <div className={classes.root}>
      <Snackbar open={true}>
        <Alert severity="error">
          Something went wrong when {props.error.context.toLowerCase()} -{" "}
          {props.error.message}
        </Alert>
      </Snackbar>
    </div>
  );
});
