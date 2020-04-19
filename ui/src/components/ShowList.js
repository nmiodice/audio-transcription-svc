import React from "react";
import { makeStyles } from "@material-ui/core/styles";

import List from "@material-ui/core/List";
import ListItem from "@material-ui/core/ListItem";
import ListItemText from "@material-ui/core/ListItemText";
import ListItemAvatar from "@material-ui/core/ListItemAvatar";
import Avatar from "@material-ui/core/Avatar";
import CircularProgress from "@material-ui/core/CircularProgress";

import { view } from "@risingstack/react-easy-state";

const useStyles = makeStyles((theme) => ({
  root: {
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    fontSize: "x-large",
    padding: theme.spacing(1, 1, 1, 1),
    backgroundColor: theme.palette.background.paper,
  },
  center: {
    height: "100%",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    fontSize: "x-large",
    padding: theme.spacing(1, 1, 1, 1),
    backgroundColor: theme.palette.background.paper,
  },
}));

export default view(function ShowList(props) {
  const classes = useStyles();
  if (props.isLoading) {
    return (
      <div className={classes.center}>
        <CircularProgress />
      </div>
    );
  }
  if (props.shows == null || props.shows.length == 0) {
    return (
      <div className={classes.center}>
        <p>Oops! Looks like there is no content...</p>
        <br />
        <div className={classes.center}>¯\_(ツ)_/¯</div>
      </div>
    );
  }

  return (
    <div className={classes.root}>
      <div>Select which podcasts to search</div>
      <List component="nav">
        {Object.keys(props.shows).map(function (id, _) {
          return (
            <ListItem button>
              <ListItemAvatar>
                <Avatar src={props.shows[id].image} />
              </ListItemAvatar>
              <ListItemText primary={props.shows[id].name} />
            </ListItem>
          );
        })}
      </List>
    </div>
  );
});
