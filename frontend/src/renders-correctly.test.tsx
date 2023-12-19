import renderer from 'react-test-renderer';
import { it, expect } from '@jest/globals';
import React from 'react';
import { BrowserRouter as Router } from 'react-router-dom';

import Button from './components/button';
import Input from './components/input';
import Navbar from './components/navbar';
import TournamentNavbar from './components/tournament-navbar';

import TournamentGame from './pages/tournament/game';
import Tournament from './pages/tournament';
import TournamentParticipants from './pages/tournament/participants';
import TournamentRanking from './pages/tournament/ranking';
import TournamentResults from './pages/tournament/results';
import TournamentRound from './pages/tournament/round';

import Create from './pages/create';
import Home from './pages';
import Login from './pages/login';
import NotFound from './pages/not-found';
import OtherUser from './pages/other-user';
import Register from './pages/register';
import User from './pages/user';

it('Button renders correctly', () => {
    const tree = renderer.create(<Button text="Test" />).toJSON();
    expect(tree).toMatchSnapshot();
});

it('Input renders correctly', () => {
    const tree = renderer.create(<Input />).toJSON();
    expect(tree).toMatchSnapshot();
});

it('Navbar renders correctly', () => {
    const tree = renderer.create(<>
        <Router>
            <Navbar />
        </Router>
    </>).toJSON();
    expect(tree).toMatchSnapshot();
});

it('TournamentNavbar renders correctly', () => {
    const tree = renderer.create(<TournamentNavbar />).toJSON();
    expect(tree).toMatchSnapshot();
});

it('TournamentGame renders correctly', () => {
    const tree = renderer.create(<TournamentGame />).toJSON();
    expect(tree).toMatchSnapshot();
});

it('Tournament renders correctly', () => {
    const tree = renderer.create(<>
        <Router>
            <Tournament />
        </Router>
    </>).toJSON();
    expect(tree).toMatchSnapshot();
});

it('TournamentParticipants renders correctly', () => {
    const tree = renderer.create(<>
        <Router>
            <TournamentParticipants />
        </Router>
    </>).toJSON();
    expect(tree).toMatchSnapshot();
});

it('TournamentRanking renders correctly', () => {
    const tree = renderer.create(<TournamentRanking />).toJSON();
    expect(tree).toMatchSnapshot();
});

it('TournamentResults renders correctly', () => {
    const tree = renderer.create(<TournamentResults />).toJSON();
    expect(tree).toMatchSnapshot();
});

it('TournamentRound renders correctly', () => {
    const tree = renderer.create(<>
        <Router>
            <TournamentRound />
        </Router>
    </>).toJSON();
    expect(tree).toMatchSnapshot();
});

it('Create renders correctly', () => {
    const tree = renderer.create(<>
        <Router>
            <Create />
        </Router>
    </>).toJSON();
    expect(tree).toMatchSnapshot();
});

it('Home renders correctly', () => {
    const tree = renderer.create(<>
        <Router>
            <Home />
        </Router>
    </>).toJSON();
    expect(tree).toMatchSnapshot();
});

it('Login renders correctly', () => {
    const tree = renderer.create(<>
        <Router>
            <Login />
        </Router>
    </>).toJSON();
    expect(tree).toMatchSnapshot();
});

it('NotFound renders correctly', () => {
    const tree = renderer.create(<NotFound />).toJSON();
    expect(tree).toMatchSnapshot();
});

it('OtherUser renders correctly', () => {
    const tree = renderer.create(<OtherUser />).toJSON();
    expect(tree).toMatchSnapshot();
});

it('Register renders correctly', () => {
    const tree = renderer.create(<>
        <Router>
            <Register />
        </Router>
    </>).toJSON();
    expect(tree).toMatchSnapshot();
});

it('User renders correctly', () => {
    const tree = renderer.create(<>
        <Router>
            <User />
        </Router>
    </>).toJSON();
    expect(tree).toMatchSnapshot();
});
