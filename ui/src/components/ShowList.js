import React from 'react';
import { makeStyles } from '@material-ui/core/styles';
import List from '@material-ui/core/List';
import ListItem from '@material-ui/core/ListItem';
import ListItemText from '@material-ui/core/ListItemText';
import ListItemAvatar from '@material-ui/core/ListItemAvatar';
import Avatar from '@material-ui/core/Avatar';

import { view } from '@risingstack/react-easy-state';

const useStyles = makeStyles((theme) => ({
    root: {
        width: '100%',
        backgroundColor: theme.palette.background.paper,
    },
}));


export default view(function ShowList(props) {
    const classes = useStyles();
    if (props == null || props.shows == null || this.props.shows.length == 0) {
        return (
            <div className={classes.root}>
                Nothing here!
            </div>
        )
    }
    return (
        <div className={classes.root}>
            <List component="nav" aria-label="main mailbox folders">
                {
                    showStore.shows.map(item => {
                        return (
                            <ListItem button>
                                <ListItemAvatar>
                                    <Avatar alt="Remy Sharp" src={item.image} />
                                </ListItemAvatar>
                                <ListItemText primary={item.name} />
                            </ListItem>
                        )
                    })
                }
            </List>
        </div>
    );
})