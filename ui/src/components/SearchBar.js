import React from 'react';
import { makeStyles } from '@material-ui/core/styles';
import { view } from '@risingstack/react-easy-state';

import InputBase from '@material-ui/core/InputBase';
import SearchIcon from '@material-ui/icons/Search';
import IconButton from '@material-ui/core/IconButton';


import Card from '@material-ui/core/Card';
import CardContent from '@material-ui/core/CardContent';



const useStyles = makeStyles((theme) => ({
    searchIcon: {
        position: 'absolute',
        left: '100%',
        height: '100%',
        transform: 'translate(-100%)',
        paddingRight: '16px',
    },
    inputRoot: {
        position: 'absolute',
        paddingLeft: '16px',
        left: '0px',
        width: '100%',
        height: '100%'
    },
    inputInput: {
        width: '90%',
        height: '100%'
    },
    cardContent: {
        height: '100%',
        padding: '0px',
        border: '0px'
    },
    root: {
        position: 'absolute',
        top: '30%',
        left: '50%',
        width: '60%',
        transform: 'translate(-50%, 50%)',
        minWidth: '400px',
        height: '50px',
        borderRadius: 8,
        boxShadow: '0px 14px 50px rgba(34, 35, 58, 0.15)',
    }
}));

export default view(function SearchBar(props) {
    const classes = useStyles();

    return (
        <Card className={classes.root} variant="outlined">
        <CardContent className={classes.cardContent}>
                <InputBase
                        autoFocus
                        placeholder={props.placeholder}
                        classes={{
                            root: classes.inputRoot,
                            input: classes.inputInput,
                        }}
                        inputProps={{ 'aria-label': 'search' }}
                    />
                <IconButton
                    edge="start"
                    className={classes.searchIcon}
                    color="inherit"
                    aria-label="GitHub">
                    <SearchIcon />
                </IconButton>
        </CardContent>
      </Card>
    );
})
