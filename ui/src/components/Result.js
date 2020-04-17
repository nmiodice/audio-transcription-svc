import React from 'react';
import { makeStyles } from '@material-ui/core/styles';
import Card from '@material-ui/core/Card';
import CardContent from '@material-ui/core/CardContent';
import CardMedia from '@material-ui/core/CardMedia';
import Divider from '@material-ui/core/Divider';
import Typography from '@material-ui/core/Typography';
import ResultTable from './ResultTable';

import { view } from '@risingstack/react-easy-state';


const useStyles = makeStyles((theme) => ({
    card: {
        display: 'flex',
        padding: theme.spacing(2),
        margin: theme.spacing(2),
        borderRadius: 4,
        textAlign: 'left',
    },
    media: {
        width: '64px',
        height: '64px',
        flexShrink: 0,
    },
    content: {
        padding: theme.spacing(0, 2, 0, 2),
        width: 'calc(100% - 64px - 8px)',
        "&:last-child": {
            paddingBottom: 0
        }
    },
    description: {
        fontSize: 12,
        color: theme.palette.grey[500],
    },
    divider: {
        margin: theme.spacing(1, 0),
    },
}));

export default view(function Result(props) {
    const classes = useStyles();
    return (
        <Card className={classes.card} variant="outlined">
            <CardMedia
                className={classes.media}
                image={props.result.media.image} />
            <CardContent className={classes.content}>
                <Typography variant="subtitle2">
                    <a href={props.result.source.homepage} target="_blank" rel="noopener noreferrer">{props.result.source.name}</a>
                </Typography>
                <Typography variant="overline" noWrap display="block">
                    {props.result.media.title}
                </Typography>
                <Typography variant="caption" className={classes.description} noWrap display="block">
                    {props.result.media.description}
                </Typography>
                <Divider className={classes.divider} light />
                <ResultTable media={props.result.media} indices={props.result.indices} query={props.query}/>
            </CardContent>
        </Card>
    );
});
