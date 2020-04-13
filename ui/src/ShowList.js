import React from 'react';
import { makeStyles } from '@material-ui/core/styles';
import List from '@material-ui/core/List';
import ListItem from '@material-ui/core/ListItem';
import ListItemText from '@material-ui/core/ListItemText';
import ListItemAvatar from '@material-ui/core/ListItemAvatar';
import Avatar from '@material-ui/core/Avatar';

import { store, view } from '@risingstack/react-easy-state';

const showStore = store({
    shows: [],
    async fetchShows() {
        showStore.shows = await fetch('/api/v1/source/')
            .then((response) => {
                console.log(response)
                return response.json();
            });
    },
});

showStore.fetchShows()


const useStyles = makeStyles((theme) => ({
    root: {
        width: '100%',
        // maxWidth: 360,
        backgroundColor: theme.palette.background.paper,
    },
}));

function ListItemLink(props) {
    return <ListItem button component="a" {...props} />;
}



function ShowList() {
    const classes = useStyles();
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
}

export default view(ShowList)