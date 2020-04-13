import React from 'react';
import { makeStyles } from '@material-ui/core/styles';
import Alert from '@material-ui/lab/Alert';
import { view } from '@risingstack/react-easy-state';

import Snackbar from '@material-ui/core/Snackbar';

const useStyles = makeStyles((theme) => ({
  root: {
    width: '100%',
  },
}));

export default view(function ErrorAlert(props) {
  const classes = useStyles();
  const [open, setOpen] = React.useState(true);

  const handleClose = (event, reason) => {
    if (reason === 'clickaway') {
      return;
    }

    setOpen(false);
  };


  if (props == null || props.error == null) {
      return null
  }

  return (
    <div className={classes.root}>
      <Snackbar open={open} autoHideDuration={6000} onClose={handleClose}>
        <Alert
            severity="error"
            onClose={handleClose}>Something went wrong when {props.error.context.toLowerCase()} - {props.error.message}</Alert>
      </Snackbar>
    </div>
  );
})
