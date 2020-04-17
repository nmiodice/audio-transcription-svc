import React from 'react';
import { makeStyles } from '@material-ui/core/styles';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableContainer from '@material-ui/core/TableContainer';
import TableRow from '@material-ui/core/TableRow';
import Paper from '@material-ui/core/Paper';
import moment from 'moment'
import Typography from '@material-ui/core/Typography';
import PlayCircleOutlineSharpIcon from '@material-ui/icons/PlayCircleOutlineSharp';
import HighlightOffIcon from '@material-ui/icons/HighlightOff';
import AudioPlayer, {RHAP_UI} from 'react-h5-audio-player';
import 'react-h5-audio-player/lib/styles.css';
import Replay10Icon from '@material-ui/icons/Replay10';
import Forward10Icon from '@material-ui/icons/Forward10';
import PauseCircleOutlineIcon from '@material-ui/icons/PauseCircleOutline';
import { view } from '@risingstack/react-easy-state';
import {state} from './../state/state'


const useStyles = makeStyles((theme) => ({
  table: {
    minWidth: 650,
  },
  contentBold: {
      fontWeight: 'bold'
  },
  playButtonCell: {
    width: '16px'
  },
  audioPlayer: {
    padding: '0px',
    boxShadow: '0px 0px',
  }
}));


function offsetToDisplayTime(offset) {
    return moment().startOf('day')
        .seconds(offset)
        .format('H:mm:ss');
}

function formatContent(content, query) {
    var regexMatch = query.split(' ').join('|')
    var regex = new RegExp("(" + regexMatch + ")","gi"); 
    return "... " + content.replace(regex, "<b>$1</b>") + "..."
}

function getAudioPlayer(classes, index, media) {
  const src = media.url + "#t=" + index.offset
  return (
    <AudioPlayer
      className={classes.audioPlayer}
      autoPlay
      src={src}
      layout='horizontal'
      progressJumpStep={10000}
      customControlsSection={[
        RHAP_UI.MAIN_CONTROLS,
        RHAP_UI.VOLUME_CONTROLS
      ]}
      customProgressBarSection={
        [
          RHAP_UI.CURRENT_TIME,
          RHAP_UI.PROGRESS_BAR,
          RHAP_UI.CURRENT_LEFT_TIME,
        ]
      }
      customIcons={{
        rewind: <Replay10Icon fontSize='inherit'/>,
        forward: <Forward10Icon fontSize='inherit'/>,
        play: <PlayCircleOutlineSharpIcon fontSize='inherit'/>,
        pause: <PauseCircleOutlineIcon fontSize='inherit'/>,
      }}
    />
  )
}

function getTableRows(classes, indices, media, query) {
  return indices.map((idx) => {
    var selected = state.playerStore.isSelected(idx)
    var row = (
      <TableRow key={idx.offset}>
        <TableCell scope="row">
          <Typography variant="caption" dangerouslySetInnerHTML={{ __html: formatContent(idx.content, query) }}/>
        </TableCell>
        <TableCell align="right">{offsetToDisplayTime(idx.offset)}</TableCell>
        <TableCell align="right" className={classes.playButtonCell}>
          {
            selected ? 
              <HighlightOffIcon onClick={(e) => state.playerStore.toggleSelection(idx)}/> :
              <PlayCircleOutlineSharpIcon onClick={(e) => state.playerStore.toggleSelection(idx)}/>
          }
        </TableCell>
      </TableRow>
    )

    if (!selected) {
      return [row]
    }

    return [
      row,
      (
        <TableRow key={idx.offset + "_player"}>
          <TableCell colspan="3">{getAudioPlayer(classes, idx, media)}</TableCell>
        </TableRow>
      )
    ]
  })
}


export default view(function ResultTable(props) {
  const classes = useStyles();
  const rows = getTableRows(classes, props.indices, props.media, props.query)

  return (
    <TableContainer component={Paper}>
      <Table className={classes.table} size="small" aria-label="simple table">
        <TableBody>
          {rows}
        </TableBody>
      </Table>
    </TableContainer>
  );
})
