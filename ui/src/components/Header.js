import React from "react";
import { makeStyles } from "@material-ui/core/styles";
import { view } from "@risingstack/react-easy-state";
import AppBar from "@material-ui/core/AppBar";
import Toolbar from "@material-ui/core/Toolbar";
import Typography from "@material-ui/core/Typography";
import IconButton from "@material-ui/core/IconButton";
import GitHubIcon from "@material-ui/icons/GitHub";

const useStyles = makeStyles((theme) => ({
  root: {
    flexGrow: 1,
  },
  menuButton: {
    marginRight: theme.spacing(2),
  },
  title: {
    paddingLeft: "16px",
    display: "none",
    [theme.breakpoints.up("sm")]: {
      display: "block",
    },
  },
  titleLink: {
    textDecoration: 'none',
    color: '#FFFFFF'
  },
  grow: {
    flexGrow: 1,
  },
}));

export default view(function Header(props) {
  const classes = useStyles();

  return (
    <div className={classes.root}>
      <AppBar position="static">
        <Toolbar>
          <a href="/">
            <img src="favicon-32x32.png" alt="logo" />
          </a>
          <Typography variant="h6" className={classes.title}>
            <a className={classes.titleLink} href="/">{props.title}</a>
          </Typography>
          <div className={classes.grow} />
          {/* <IconButton
            edge="start"
            className={classes.menuButton}
            color="inherit"
            aria-label="GitHub"
            href={props.github_link}
          >
            <GitHubIcon />
          </IconButton> */}
        </Toolbar>
      </AppBar>
    </div>
  );
});
